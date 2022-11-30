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

file = sys.argv[1]
with open(file, 'r') as f: 
    contents = f.read()

relative = 'file:/p2/'
absolute = f"file:/" + os.path.abspath('p2').replace('\\', '/') + '/'

if sys.argv[2] == 'absolute':
    contents = contents.replace(relative, absolute)
else:
    contents = contents.replace(absolute, relative)

with open(file, 'w') as f:
    f.write(contents)
