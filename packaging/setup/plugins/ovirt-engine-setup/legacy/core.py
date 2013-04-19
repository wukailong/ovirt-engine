#
# ovirt-engine-setup -- ovirt engine setup
# Copyright (C) 2013 Red Hat, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#


"""Detect upgrade from legacy plugin."""


import os
import gettext
_ = lambda m: gettext.dgettext(message=m, domain='ovirt-engine-setup')


from otopi import util
from otopi import plugin


from ovirt_engine_setup import constants as osetupcons


@util.export
class Plugin(plugin.PluginBase):
    """Detect upgrade from legacy plugin."""

    def __init__(self, context):
        super(Plugin, self).__init__(context=context)

    @plugin.event(
        stage=plugin.Stages.STAGE_INIT,
    )
    def _init(self):
        if self.environment[osetupcons.CoreEnv.DEVELOPER_MODE]:
            self.environment[osetupcons.CoreEnv.UPGRADE_FROM_LEGACY] = False
        else:
            self.environment[osetupcons.CoreEnv.UPGRADE_FROM_LEGACY] = (
                os.path.exists(
                    osetupcons.FileLocations.OVIRT_ENGINE_PKI_ENGINE_CA_CERT
                ) and
                not os.path.exists(
                    osetupcons.FileLocations.OVIRT_SETUP_POST_INSTALL_CONFIG
                )
            )


# vim: expandtab tabstop=4 shiftwidth=4