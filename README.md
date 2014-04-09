com.genuitec.mobione.phoneui
==============

The native part of Mobione phoneui framework.

Phonegap plugin that allows to build applications compiled by MobiOne Studio http://www.genuitec.com/mobile/
Allows to build apps generated by MobiOne with Phonegap CLI and Phonegap Build.

usage
==============

Plugin exposes functions which is not intended for direct usage but required for MobiOne phoneui framework.

1. Generate application with MobiOne
2. Create Phonegap application, add mobione-plugin to it:
    `phonegap create <path>`
    `cd <path>`
    `phonegap plugin add http://github.com/genuitec-mobione/mobione-plugin.git`
3. Copy generated app's www folder to Phonegap project www folder
4. do `phonegap build`

supported platforms
==============

iOS


license
==============

Plugin code is available under Apache 2.0 License 
