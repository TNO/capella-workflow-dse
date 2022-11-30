#
# Copyright (c) 2022 ESI (TNO)
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#

import sys, platform, venv, os, shutil, subprocess, zipfile, urllib.request, tarfile

if sys.version_info < (3,5):
    print("Sorry, requires at least Python 3.5, got %s" % platform.python_version())
    sys.exit(1)

class _EnvBuilder(venv.EnvBuilder):
    def __init__(self, *args, **kwargs):
        self.context = None
        super().__init__(*args, **kwargs)

    def post_setup(self, context):
        self.context = context

upx_path = 'upx'
shutil.rmtree(upx_path, ignore_errors=True)
upx_lookup = {"Windows": ('4.0.1', 'upx-4.0.1-win64.zip'), 'Linux': ('4.0.1', 'upx-4.0.1-amd64_linux.tar.xz')}
has_upx = platform.system() in upx_lookup
if has_upx:
    print("Downloading upx")
    upx_url = "https://github.com/upx/upx/releases/download/v%s/%s" % upx_lookup[platform.system()]
    archive_path, _ = urllib.request.urlretrieve(upx_url)
    if upx_url.endswith('.zip'):
        with zipfile.ZipFile(archive_path, "r") as f:
            f.extractall(upx_path)
    else:
        with tarfile.open(archive_path) as f:
            f.extractall(upx_path)
    upx_path = os.path.join(upx_path, os.listdir(upx_path)[0])

print("Creating venv...")
builder = _EnvBuilder(with_pip=True)
venv_path = os.path.join(os.path.dirname(__file__), "venv_builder")
shutil.rmtree(venv_path, ignore_errors=True)
builder.create(venv_path)
context = builder.context

print("Installing requirements...")
subprocess.check_call([context.env_exe, '-m' , 'pip', 'install', '-r', 'requirements.txt'])

print ("Building...")
is_mac = platform.system() == 'Darwin'
extra_cmd = []
if has_upx: extra_cmd.extend(['--upx-dir', upx_path])
if is_mac: extra_cmd.append('--onefile') # Required on mac otherwise executable gives exec format error
subprocess.check_call([context.env_exe, '-m' , 'PyInstaller', *extra_cmd, '--noconfirm', 'python.py'])

print('Copying...')
target_lookup = {
    'Windows': 'python-win32-x64', 
    'Linux': 'python-linux-x64',
    'Darwin': 'python-macosx-x64'
}
target_dir = f"../dist/{target_lookup[platform.system()]}"
shutil.rmtree(target_dir, ignore_errors=True)
os.makedirs(target_dir, exist_ok=True)
shutil.copytree('dist/python', target_dir, dirs_exist_ok=True)

print("Done")