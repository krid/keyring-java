Desktop client for managing Keyring for webOS files

Keyring for webOS stores your account information securely, so you don't
have to rely on your memory, or on little scraps of paper. Sensitive data
are encrypted using the Blowfish algorithm, and protected by a master password.

Features:

*) Load and save Keyring databases via the file system or URL
*) View, edit, add and delete items
*) Import from the following formats:
 - Keyring for PalmOS
 - eWallet
 - CodeWallet
 - CSV (plain and Excel)
*) Export to CSV (round trip from CSV => Keyring => CSV with no differences)
*) Automatic lockout after 60s idle time.

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
    GNU General Public License for more details (in COPYING.txt)

This application includes code from several sources:

The GUI is based on KeyringEditor v1.1
Copyright 2006 Markus Griessnig
http://www.ict.tuwien.ac.at/keyring/
Markus graciously gave his assent to release the modified code under the GPLv3.

eWallet, CodeWallet and CSV conversion code
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