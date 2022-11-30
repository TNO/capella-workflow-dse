#
# Copyright (c) 2022 ESI (TNO)
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#

# Add all required dependencies here
import snakes.nets
import snakes.plugins.gv
import snakes.plugins.pos
import snakes.plugins.clusters

import sys
with open(sys.argv[1], 'r') as file:
    exec(file.read())
