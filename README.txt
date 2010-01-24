Java utilities supporting Keyring for webOS

Keyring for webOS stores your account information securely, so you don't
have to rely on your memory, or on little scraps of paper. Sensitive data
are encrypted using the Blowfish algorithm, and protected by a master password.

This package currently supports converting the following formats to Keyring
for webOS:
 - Keyring for PalmOS
 - eWallet
 - CodeWallet
 - CSV (plain and Excel)
It will eventually support viewing of Keyring items on your PC.

More information and instructions for use can be found at
http://quux.otisbean.com/keyring/.

http://github.com/krid/keyring-java/

LICENSE

Keyring for webOS is Copyright (C) 2009-2010, Dirk Bergstrom

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

The Java utilities include code from several sources:

eWallet, CodeWallet and CSV converters
Contributed by Matt Williams

Ostermiller CSV/Excel libraries - GPL
Copyright (C) 2002-2004 Stephen Ostermiller
http://ostermiller.org/

iHarder Base64 converter - Public Domain
Robert Harder
http://iharder.net/base64

AstroInfo PDB format library - GPL
Copyright (C) 2002, Astro Info SourceForge Project
http://astroinfo.sourceforge.net/

json-simple - Apache 2.0 license
FangYidong<fangyidong@yahoo.com.cn>
http://code.google.com/p/json-simple/

Java Keyring for PalmOS library - GPL
By Jay Dickon Glanville and others
http://gnukeyring.sourceforge.net/conduits.html
http://gnukeyring.svn.sourceforge.net/viewvc/gnukeyring/trunk/keyring-link/java/