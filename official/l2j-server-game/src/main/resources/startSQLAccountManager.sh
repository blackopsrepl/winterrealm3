#!/bin/sh

java -Djava.util.logging.config.file=config/console.cfg -cp l2jserver.jar net.sf.l2j.accountmanager.SQLAccountManager
