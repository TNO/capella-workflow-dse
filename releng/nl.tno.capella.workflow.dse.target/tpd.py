#
# Copyright (c) 2022 ESI (TNO)
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#

import sys, os

in_file = sys.argv[1]
out_file = sys.argv[2]

with open(in_file, 'r') as f: 
    contents = f.read()

relative = 'file:/p2/'
absolute = f"file:/" + os.path.abspath('p2').replace('\\', '/') + '/'
contents = contents.replace(relative, absolute)

with open(out_file, 'w') as f:
    f.write(contents)
