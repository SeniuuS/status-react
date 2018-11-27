(ns status-im.hardwallet.fx
  (:require [re-frame.core :as re-frame]
            [status-im.hardwallet.card :as card]))

(re-frame/reg-fx
 :hardwallet/get-application-info
 card/get-application-info)

(re-frame/reg-fx
 :hardwallet/check-nfc-support
 card/check-nfc-support)

(re-frame/reg-fx
 :hardwallet/check-nfc-enabled
 card/check-nfc-enabled)

(re-frame/reg-fx
 :hardwallet/open-nfc-settings
 card/open-nfc-settings)

(re-frame/reg-fx
 :hardwallet/start-module
 card/start)

(re-frame/reg-fx
 :hardwallet/initialize-card
 card/initialize)

(re-frame/reg-fx
 :hardwallet/register-card-events
 card/register-card-events)

(re-frame/reg-fx
 :hardwallet/pair
 card/pair)

(re-frame/reg-fx
 :hardwallet/generate-mnemonic
 card/generate-mnemonic)

(re-frame/reg-fx
 :hardwallet/save-mnemonic
 card/save-mnemonic)

(re-frame/reg-fx
 :hardwallet/generate-and-load-key
 card/generate-and-load-key)

(re-frame/reg-fx
 :hardwallet/derive-key
 card/derive-key)

(re-frame/reg-fx
 :hardwallet/export-key
 card/export-key)
