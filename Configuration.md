# Winlink Form Viewer (fv) Configuration

This document describes all  the configuration options available for the fv system

## conf/fv.conf
This is the primary configuration file for fv. Parameters are in a key=value format

- forms.path -- this is where the Standard Templates are stored. If you are on a Windows platform with Winlink Express running, it is perfectly reasonable and probably **preferable** to use the Winlink Express Templates, typically C:\RMS Express\Standard Templates
- usage.file -- a short text file that is used to supply "usage" information if the fv program is not able to successfully start
- server.port -- the IP port that the fv server will listen to for requests. You may need to change this value if another server is running on the specified port (default = 6676)
- server.initialHtml -- the HTML file that contains the "main" page for the fv app. Javascript and CSS styling are embedded.
- server.initialView -- the initial "view" file that is displayed on the initial page. This is to provide some visual guidance to new users.
- server.404Html -- the HTML file for providing a 404 response back to the user

- forms.update.url.prefix -- used to construct the request to check for latest versions of Standard Templates. Provided if/when things change
- forms.update.url.magic -- a small piece of recognizable text to find the one link on the update page that points to the latest version of the Standard Templates. This is very brittle and liable to break

- emsg.* -- text for various error messages that can be customized or translated for local needs.

## conf/logback.xml
This file controls the logging (output) from the fv program. 
- Aappenders are the "sinks" for logging information; it's where logging messages show up. 
- Patterns describe which fields are written
- Loggers are the "sources" for logging information. Loggers have minimum levels. Turn level to DEBUG and be overwhelmed. There are three loggers defined:
1. serverLogger -- for the "access log" for the server
2. com.surftools -- for all of the fv software
3. root -- for all the third-party software. Log level is set to WARN

## conf/sample-view-files/
one or more sample "view' files to demonstrate how to use fv