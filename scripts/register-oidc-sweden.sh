#!/usr/bin/env bash
#
# Copyright 2026 OIDC Sweden
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# ---------------------------------------------------------------------------
#
# Registers the OIDC Sweden client scopes and user profile attributes in a
# Keycloak realm, through the Admin REST API.
#
# The oidc-sweden-claims-plugin JAR registers the two protocol mapper types and
# nothing else. Everything this script does is what an operator would otherwise
# do by hand in the Admin Console. It is meant to be read and adapted, not
# treated as a black box; every request it makes is written out plainly below.
#
# What it does, in order:
#
#   1. Verifies that the two OIDC Sweden protocol mapper types are known to the
#      server, i.e. that the JAR is deployed and `kc.sh build` has been run.
#   2. Creates the two user profile attribute groups (before the attributes,
#      which reference the groups by name).
#   3. Creates the ten user profile attributes.
#   4. Creates the three client scopes, each with its mapper attached, emitting
#      into the ID token, the access token and the UserInfo response.
#
# Everything is idempotent: anything already present is reported and left
# exactly as it is. Running the script twice changes nothing the second time.
#
# Requires: bash 4+, curl, jq.
#
# ---------------------------------------------------------------------------

set -euo pipefail

# --- Defaults --------------------------------------------------------------

KC_URL=""
KC_PATH_PREFIX=""
KC_REALM=""
KC_ADMIN_REALM="master"
KC_ADMIN_CLIENT_ID="admin-cli"
KC_ADMIN_USER=""
KC_ADMIN_PASSWORD="${KEYCLOAK_ADMIN_PASSWORD:-}"
KC_CACERT=""

usage() {
  cat <<'EOF'
Registers the OIDC Sweden client scopes and user profile attributes in a Keycloak realm.

Usage:
  register-oidc-sweden.sh --url <base-url> --realm <realm> --user <admin> [options]

Required:
  --url <base-url>        Keycloak base URL, e.g. https://kc.example.com
  --realm <realm>         The realm to configure, e.g. my-realm
  --user <username>       Admin username

Options:
  --password <password>   Admin password. If omitted, the KEYCLOAK_ADMIN_PASSWORD
                          environment variable is used, or the script prompts for it.
  --path-prefix <path>    URL path prefix Keycloak is served under, e.g. /auth
                          (Keycloak's http-relative-path). Default: none.
  --admin-realm <realm>   Realm to authenticate the admin user against. Default: master
  --admin-client <id>     Client ID used for the admin login. Default: admin-cli
  --cacert <file>         PEM file with the CA certificate to verify the Keycloak
                          TLS certificate against, for a private or internal CA.
  -h, --help              Show this help.

Example:
  ./register-oidc-sweden.sh \
      --url https://kc.example.com --path-prefix /auth \
      --realm my-realm --user admin \
      --cacert /etc/ssl/certs/internal-ca.pem
EOF
}

# --- Argument parsing ------------------------------------------------------

while [[ $# -gt 0 ]]; do
  case "$1" in
    --url)           KC_URL="$2"; shift 2 ;;
    --realm)         KC_REALM="$2"; shift 2 ;;
    --user)          KC_ADMIN_USER="$2"; shift 2 ;;
    --password)      KC_ADMIN_PASSWORD="$2"; shift 2 ;;
    --path-prefix)   KC_PATH_PREFIX="$2"; shift 2 ;;
    --admin-realm)   KC_ADMIN_REALM="$2"; shift 2 ;;
    --admin-client)  KC_ADMIN_CLIENT_ID="$2"; shift 2 ;;
    --cacert)        KC_CACERT="$2"; shift 2 ;;
    -h|--help)       usage; exit 0 ;;
    *)               echo "Unknown argument: $1" >&2; usage >&2; exit 2 ;;
  esac
done

die() {
  echo "ERROR: $*" >&2
  exit 1
}

[[ -n "${KC_URL}" ]]        || { usage >&2; die "--url is required"; }
[[ -n "${KC_REALM}" ]]      || { usage >&2; die "--realm is required"; }
[[ -n "${KC_ADMIN_USER}" ]] || { usage >&2; die "--user is required"; }

command -v curl >/dev/null || die "curl is required but was not found on PATH"
command -v jq   >/dev/null || die "jq is required but was not found on PATH"

if [[ -z "${KC_ADMIN_PASSWORD}" ]]; then
  read -r -s -p "Password for '${KC_ADMIN_USER}': " KC_ADMIN_PASSWORD
  echo
fi
[[ -n "${KC_ADMIN_PASSWORD}" ]] || die "no admin password given"

# Normalise: strip trailing slash from the base URL, ensure the prefix starts
# with a slash and does not end with one. "https://host" + "/auth" is then the
# base every request below is built from.
KC_URL="${KC_URL%/}"
if [[ -n "${KC_PATH_PREFIX}" ]]; then
  [[ "${KC_PATH_PREFIX}" == /* ]] || KC_PATH_PREFIX="/${KC_PATH_PREFIX}"
  KC_PATH_PREFIX="${KC_PATH_PREFIX%/}"
fi
BASE="${KC_URL}${KC_PATH_PREFIX}"

if [[ -n "${KC_CACERT}" ]]; then
  [[ -r "${KC_CACERT}" ]] || die "CA certificate file not readable: ${KC_CACERT}"
fi

# --- HTTP helpers ----------------------------------------------------------

# curl invocation shared by every request: fail quietly on transport errors,
# no progress meter, and the CA certificate if one was given.
curl_base() {
  local args=(--silent --show-error)
  if [[ -n "${KC_CACERT}" ]]; then
    args+=(--cacert "${KC_CACERT}")
  fi
  curl "${args[@]}" "$@"
}

# api <METHOD> <PATH> [BODY]
#
# Calls the Admin REST API and echoes "<http-status>\n<body>". The caller
# decides what to make of the status, and several of the calls below treat 409
# (already exists) as success.
api() {
  local method="$1" path="$2" body="${3:-}"
  local args=(-X "${method}"
              -H "Authorization: Bearer ${ACCESS_TOKEN}"
              -H "Accept: application/json"
              --write-out '\n%{http_code}')
  if [[ -n "${body}" ]]; then
    args+=(-H "Content-Type: application/json" --data "${body}")
  fi
  curl_base "${args[@]}" "${BASE}/admin/realms/${KC_REALM}${path}"
}

# Splits the "<body>\n<status>" produced by api() into the globals
# RESP_BODY and RESP_STATUS.
call_api() {
  local out
  out="$(api "$@")"
  RESP_STATUS="${out##*$'\n'}"
  RESP_BODY="${out%$'\n'*}"
}

ok_status() {
  [[ "$1" =~ ^2[0-9][0-9]$ ]]
}

# --- Counters, for the closing summary -------------------------------------

CREATED=0
EXISTING=0

created() {
  echo "  + created  $*"
  CREATED=$((CREATED + 1))
}

unchanged() {
  echo "  = present  $*"
  EXISTING=$((EXISTING + 1))
}

# --- 0. Authenticate -------------------------------------------------------

echo "Keycloak      : ${BASE}"
echo "Realm         : ${KC_REALM}"
echo "Admin         : ${KC_ADMIN_USER} (realm '${KC_ADMIN_REALM}', client '${KC_ADMIN_CLIENT_ID}')"
echo

TOKEN_RESPONSE="$(curl_base -X POST \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data-urlencode "grant_type=password" \
  --data-urlencode "client_id=${KC_ADMIN_CLIENT_ID}" \
  --data-urlencode "username=${KC_ADMIN_USER}" \
  --data-urlencode "password=${KC_ADMIN_PASSWORD}" \
  "${BASE}/realms/${KC_ADMIN_REALM}/protocol/openid-connect/token")" \
  || die "could not reach ${BASE}, check --url, --path-prefix and --cacert"

ACCESS_TOKEN="$(jq -r '.access_token // empty' <<<"${TOKEN_RESPONSE}")"
[[ -n "${ACCESS_TOKEN}" ]] || die "admin login failed: ${TOKEN_RESPONSE}"

# --- 1. Verify that the protocol mapper types are deployed -----------------
#
# This is the check that tells a missing or unbuilt JAR apart from a
# misconfigured realm. /admin/serverinfo lists every protocol mapper type the
# server knows about; it needs privileges on the admin realm, so if it is not
# available we fall back to the plugin's own unauthenticated info endpoint,
# which answers only when the JAR is loaded.

CLAIMS_MAPPER="oidc-sweden-claims-mapper"
INFO_MAPPER="natural-person-info-mapper"

echo "Checking that the OIDC Sweden protocol mapper types are deployed"

MAPPER_IDS=""
SERVERINFO="$(curl_base -H "Authorization: Bearer ${ACCESS_TOKEN}" -H "Accept: application/json" \
  --write-out '\n%{http_code}' "${BASE}/admin/serverinfo")"
SERVERINFO_STATUS="${SERVERINFO##*$'\n'}"
SERVERINFO_BODY="${SERVERINFO%$'\n'*}"

if ok_status "${SERVERINFO_STATUS}"; then
  MAPPER_IDS="$(jq -r '.protocolMapperTypes["openid-connect"][]?.id' <<<"${SERVERINFO_BODY}")"
else
  # No access to /admin/serverinfo, so ask the plugin itself.
  PLUGIN_INFO="$(curl_base -H "Accept: application/json" --write-out '\n%{http_code}' \
    "${BASE}/realms/${KC_REALM}/oidc-sweden/info")"
  if ok_status "${PLUGIN_INFO##*$'\n'}"; then
    MAPPER_IDS="$(jq -r '.protocolMappers[]?' <<<"${PLUGIN_INFO%$'\n'*}")"
  fi
fi

for mapper in "${CLAIMS_MAPPER}" "${INFO_MAPPER}"; do
  if ! grep -qx -- "${mapper}" <<<"${MAPPER_IDS}"; then
    die "protocol mapper type '${mapper}' is not known to this Keycloak.

The oidc-sweden-claims-plugin JAR is not deployed, or Keycloak has not been
rebuilt since it was added. On the Keycloak host:

    cp oidc-sweden-claims-plugin-<version>.jar /opt/keycloak/providers/
    /opt/keycloak/bin/kc.sh build
    /opt/keycloak/bin/kc.sh start --optimized

Then run this script again."
  fi
done
echo "  = present  both protocol mapper types"
echo

# --- 2 & 3. User profile groups and attributes -----------------------------
#
# The user profile schema is read and written as a whole document, so groups
# and attributes are merged into the current configuration and PUT back in one
# request. Groups are added first: an attribute referencing a group that does
# not exist is rejected.

echo "User profile groups and attributes"

call_api GET "/users/profile"
ok_status "${RESP_STATUS}" || die "could not read the user profile configuration (HTTP ${RESP_STATUS}): ${RESP_BODY}"
UP_CONFIG="${RESP_BODY}"

# The two attribute groups, exactly as defined in SwedishOidcAttributes.groups().
GROUPS_JSON='[
  {
    "name": "oidc-sweden-natural-person",
    "displayHeader": "OIDC Sweden: Natural Person",
    "displayDescription": "Swedish personal identity number and coordination number attributes per the Swedish OIDC Claims Specification.",
    "annotations": {}
  },
  {
    "name": "oidc-sweden-org-id",
    "displayHeader": "OIDC Sweden: Organisational Identity",
    "displayDescription": "Swedish organisational identity attributes per the Swedish OIDC Claims Specification.",
    "annotations": {}
  }
]'

# The ten attributes, exactly as defined in SwedishOidcAttributes.all():
# viewable by admin and user, editable by admin only, single-valued, not
# required. "middleName" and "birthdate" deliberately have no group.
ATTRIBUTES_JSON='[
  { "name": "middleName",                 "displayName": "Middle Name",                 "group": null },
  { "name": "birthdate",                  "displayName": "Date of Birth",               "group": null },
  { "name": "personalIdentityNumber",     "displayName": "Personal Identity Number",    "group": "oidc-sweden-natural-person" },
  { "name": "coordinationNumber",         "displayName": "Coordination Number",         "group": "oidc-sweden-natural-person" },
  { "name": "coordinationNumberLevel",    "displayName": "Coordination Number Level",   "group": "oidc-sweden-natural-person" },
  { "name": "previousCoordinationNumber", "displayName": "Previous Coordination Number","group": "oidc-sweden-natural-person" },
  { "name": "orgAffiliation",             "displayName": "Organizational Affiliation",  "group": "oidc-sweden-org-id" },
  { "name": "orgName",                    "displayName": "Organization Name",           "group": "oidc-sweden-org-id" },
  { "name": "orgNumber",                  "displayName": "Organization Number",         "group": "oidc-sweden-org-id" },
  { "name": "orgUnit",                    "displayName": "Organizational Unit",         "group": "oidc-sweden-org-id" }
]'

# Report what is about to change, before changing it.
for name in $(jq -r '.[].name' <<<"${GROUPS_JSON}"); do
  if jq -e --arg n "${name}" '(.groups // []) | any(.name == $n)' >/dev/null <<<"${UP_CONFIG}"; then
    unchanged "group     ${name}"
  else
    created "group     ${name}"
  fi
done

for name in $(jq -r '.[].name' <<<"${ATTRIBUTES_JSON}"); do
  if jq -e --arg n "${name}" '(.attributes // []) | any(.name == $n)' >/dev/null <<<"${UP_CONFIG}"; then
    unchanged "attribute ${name}"
  else
    created "attribute ${name}"
  fi
done

# Merge: append only what is missing, never touch what is there. Attributes
# get the permissions from SwedishOidcAttributes; the "group" key is dropped
# for the two ungrouped attributes.
UPDATED_CONFIG="$(jq \
  --argjson groups "${GROUPS_JSON}" \
  --argjson attributes "${ATTRIBUTES_JSON}" '
  . as $config
  | ($config.groups // []) as $existingGroups
  | ($config.attributes // []) as $existingAttributes
  | ($groups
      | map(select(. as $g | $existingGroups | any(.name == $g.name) | not))) as $newGroups
  | ($attributes
      | map(select(. as $a | $existingAttributes | any(.name == $a.name) | not))
      | map({
          name: .name,
          displayName: .displayName,
          multivalued: false,
          permissions: { view: ["admin", "user"], edit: ["admin"] },
          validations: {},
          annotations: {}
        } + (if .group == null then {} else { group: .group } end))) as $newAttributes
  | $config
    + { groups: ($existingGroups + $newGroups) }
    + { attributes: ($existingAttributes + $newAttributes) }
  ' <<<"${UP_CONFIG}")"

if [[ "$(jq -cS . <<<"${UPDATED_CONFIG}")" == "$(jq -cS . <<<"${UP_CONFIG}")" ]]; then
  echo "  user profile configuration already complete, not written"
else
  call_api PUT "/users/profile" "${UPDATED_CONFIG}"
  ok_status "${RESP_STATUS}" \
    || die "could not write the user profile configuration (HTTP ${RESP_STATUS}): ${RESP_BODY}"
  echo "  user profile configuration updated"
fi
echo

# --- 4. Client scopes ------------------------------------------------------
#
# One client scope per OIDC Sweden scope, each carrying the protocol mapper
# that emits its claims into the ID token, the access token and UserInfo.
# Scope names are the specification URIs.

echo "Client scopes"

call_api GET "/client-scopes"
ok_status "${RESP_STATUS}" || die "could not list client scopes (HTTP ${RESP_STATUS}): ${RESP_BODY}"
EXISTING_SCOPES="${RESP_BODY}"

# name | description | mapper provider id | mapper instance name
SCOPES=(
  "https://id.oidc.se/scope/naturalPersonInfo|Natural person information (given_name, family_name, middle_name, name, birthdate)|${INFO_MAPPER}|natural-person-info-mapper"
  "https://id.oidc.se/scope/naturalPersonNumber|Swedish personal identity number or coordination number|${CLAIMS_MAPPER}|oidc-sweden-claims-mapper"
  "https://id.oidc.se/scope/naturalPersonOrgId|Swedish organizational identity (orgAffiliation, orgName, orgNumber, orgUnit)|${CLAIMS_MAPPER}|oidc-sweden-claims-mapper"
)

for entry in "${SCOPES[@]}"; do
  IFS='|' read -r scope_name description mapper_provider mapper_name <<<"${entry}"

  if jq -e --arg n "${scope_name}" 'any(.name == $n)' >/dev/null <<<"${EXISTING_SCOPES}"; then
    unchanged "scope     ${scope_name}"
    # Left untouched, but say so if it is not carrying the mapper, since that
    # scope will not emit any OIDC Sweden claim.
    scope_id="$(jq -r --arg n "${scope_name}" '.[] | select(.name == $n) | .id' <<<"${EXISTING_SCOPES}")"
    call_api GET "/client-scopes/${scope_id}/protocol-mappers/models"
    if ok_status "${RESP_STATUS}" \
        && ! jq -e --arg m "${mapper_provider}" 'any(.protocolMapper == $m)' >/dev/null <<<"${RESP_BODY}"; then
      echo "    NOTE: this scope has no '${mapper_provider}' mapper. It was left untouched;"
      echo "          add the mapper by hand if the scope is meant to emit OIDC Sweden claims."
    fi
    continue
  fi

  payload="$(jq -n \
    --arg name "${scope_name}" \
    --arg description "${description}" \
    --arg mapperName "${mapper_name}" \
    --arg mapperProvider "${mapper_provider}" '
    {
      name: $name,
      description: $description,
      protocol: "openid-connect",
      attributes: {
        "include.in.token.scope": "true",
        "display.on.consent.screen": "true"
      },
      protocolMappers: [
        {
          name: $mapperName,
          protocol: "openid-connect",
          protocolMapper: $mapperProvider,
          config: {
            "id.token.claim": "true",
            "access.token.claim": "true",
            "userinfo.token.claim": "true"
          }
        }
      ]
    }')"

  call_api POST "/client-scopes" "${payload}"
  if ok_status "${RESP_STATUS}"; then
    created "scope     ${scope_name} (mapper '${mapper_name}')"
  elif [[ "${RESP_STATUS}" == "409" ]]; then
    # Created by someone else between the listing and this request.
    unchanged "scope     ${scope_name}"
  else
    die "could not create client scope '${scope_name}' (HTTP ${RESP_STATUS}): ${RESP_BODY}"
  fi
done
echo

# --- Summary ---------------------------------------------------------------

echo "Done. ${CREATED} created, ${EXISTING} already present."
if [[ "${CREATED}" -gt 0 ]]; then
  cat <<EOF

The scopes are now defined in realm '${KC_REALM}'. Assign them to the clients
that should be able to request them:

  Clients → <client> → Client scopes → Add client scope → select the
  https://id.oidc.se/scope/... scopes → Add as Optional
EOF
fi
