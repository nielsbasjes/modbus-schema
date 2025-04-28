#!/bin/bash
#
# Copyright (C) 2025 Niels Basjes
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an AS IS BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

SCRIPTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"

echo "PWD: ${SCRIPTDIR}"
cd "${SCRIPTDIR}" || ( echo "This should not be possible" ; exit 1 )

USER=$(id -un)

if [ "$(uname -s)" == "Linux" ]; then
  USER_NAME=${SUDO_USER:=${USER}}
  USER_ID=$(id -u "${USER_NAME}")
  GROUP_ID=$(id -g "${USER_NAME}")
else # boot2docker uid and gid
  USER_NAME=${USER}
  USER_ID=1000
  GROUP_ID=50
fi

cat - > ___UserSpecificDockerfile << UserSpecificDocker
FROM renovate/renovate:latest
USER root

# If you are on a normal Ubuntu then you have uid 1000 ... which is already in use in the Ubuntu image
RUN groupadd --non-unique -g "${GROUP_ID}" "${USER_NAME}"
RUN useradd -g "${GROUP_ID}" -u "${USER_ID}" -k /root -m "${USER_NAME}"
RUN chown "${USER_NAME}" "/home/${USER_NAME}"
RUN echo "export HOME=/home/${USER_NAME}" >> "/home/${USER_NAME}/.bashrc"
RUN echo "export USER=${USER_NAME}" >> "/home/${USER_NAME}/.bashrc"

ENV LOG_LEVEL="debug"
UserSpecificDocker

docker build -t "renovate-local-${USER_NAME}" -f ___UserSpecificDockerfile .

buildStatus=$?

if [ ${buildStatus} -ne 0 ];
then
    echo "Building the user specific docker image failed."
    exit ${buildStatus}
fi

rm -f ___UserSpecificDockerfile

echo ""
echo "Docker image build completed."
echo "=============================================================================================="
echo ""

# man docker-run
# When using SELinux, mounted directories may not be accessible
# to the container. The z mount option fixes this automatically.
V_OPTS=:z

docker run --rm=true -t -i                                      \
       -u "${USER_NAME}"                                        \
       -v "${HOME}/.m2:/home/${USER_NAME}/.m2${V_OPTS:-}"       \
       -v "${PWD}:/home/${USER_NAME}/toberenovated${V_OPTS:-}"  \
       -w "/home/${USER}/toberenovated"                         \
       "renovate-local-${USER_NAME}"                            \
       bash -c "renovate --platform=local --dry-run=full"

exit 0
