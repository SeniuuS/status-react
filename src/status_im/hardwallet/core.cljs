(ns status-im.hardwallet.core
  (:require [re-frame.core :as re-frame]
            status-im.hardwallet.fx
            [status-im.ui.screens.navigation :as navigation]
            [status-im.utils.config :as config]
            [status-im.utils.fx :as fx]
            [status-im.utils.platform :as platform]
            [taoensso.timbre :as log]
            [status-im.native-module.core :as status]
            [status-im.utils.types :as types]
            [status-im.constants :as constants]
            [status-im.utils.identicon :as identicon]
            [status-im.utils.gfycat.core :as gfycat]
            [status-im.data-store.accounts :as accounts-store]
            [status-im.utils.hex :as utils.hex]
            [clojure.string :as string]
            [status-im.i18n :as i18n]
            [status-im.accounts.login.core :as accounts.login]))

(defn hardwallet-supported? [{:keys [db]}]
  (and config/hardwallet-enabled?
       platform/android?
       (get-in db [:hardwallet :nfc-supported?])))

(fx/defn on-application-info-success [{:keys [db]} info]
  (let [info' (js->clj info :keywordize-keys true)]
    {:db (-> db
             (assoc-in [:hardwallet :application-info] info')
             (assoc-in [:hardwallet :application-info-error] nil))}))

(fx/defn on-application-info-error
  [{:keys [db]} error]
  (log/debug "[hardwallet] application info error " error)
  {:db (assoc-in db [:hardwallet :application-info-error] error)})

(fx/defn set-nfc-support
  [{:keys [db]} supported?]
  {:db (assoc-in db [:hardwallet :nfc-supported?] supported?)})

(fx/defn set-nfc-enabled
  [{:keys [db]} enabled?]
  {:db (assoc-in db [:hardwallet :nfc-enabled?] enabled?)})

(fx/defn navigate-to-connect-screen [cofx]
  (fx/merge cofx
            {:hardwallet/check-nfc-enabled  nil
             :hardwallet/register-card-events nil}
            (navigation/navigate-to-cofx :hardwallet-connect nil)))

(fx/defn success-button-pressed [cofx]
  (fx/merge cofx
            ;(accounts.login/user-login cofx)
))

(fx/defn pair [{:keys [db] :as cofx}]
  {:db              (assoc-in db [:hardwallet :setup-step] :pairing)
   :hardwallet/pair cofx})

(fx/defn return-back-from-nfc-settings [{:keys [db]}]
  (when (= :hardwallet-connect (:view-id db))
    {:hardwallet/check-nfc-enabled nil}))

(defn- proceed-to-pin-confirmation [fx]
  (assoc-in fx [:db :hardwallet :pin :enter-step] :confirmation))

(defn- pin-match [{:keys [db]}]
  {:db                   (assoc-in db [:hardwallet :pin :status] :validating)
   :utils/dispatch-later [{:ms       3000
                           :dispatch [:hardwallet.callback/on-pin-validated]}]})

(defn- pin-mismatch [fx]
  (assoc-in fx [:db :hardwallet :pin] {:status       :error
                                       :error        :t/pin-mismatch
                                       :original     []
                                       :confirmation []
                                       :enter-step   :original}))

(fx/defn process-pin-input
  [{:keys [db]} number enter-step]
  (let [db' (update-in db [:hardwallet :pin enter-step] conj number)
        numbers-entered (count (get-in db' [:hardwallet :pin enter-step]))]
    (cond-> {:db (assoc-in db' [:hardwallet :pin :status] nil)}
      (and (= enter-step :original)
           (= 6 numbers-entered))
      (proceed-to-pin-confirmation)

      (and (= enter-step :confirmation)
           (= (get-in db' [:hardwallet :pin :original])
              (get-in db' [:hardwallet :pin :confirmation])))
      (pin-match)

      (and (= enter-step :confirmation)
           (= 6 numbers-entered)
           (not= (get-in db' [:hardwallet :pin :original])
                 (get-in db' [:hardwallet :pin :confirmation])))
      (pin-mismatch))))

(fx/defn generate-mnemonic
  [{:keys [db] :as cofx}]
  (fx/merge cofx
            {:db                           (assoc-in db [:hardwallet :setup-step] :generating-mnemonic)
             :hardwallet/generate-mnemonic cofx}))

(fx/defn on-card-connected
  [{:keys [db] :as cofx} data]
  (let [data' (js->clj data :keywordize-keys true)
        payload (get-in data' [:ndefMessage 0 :payload])]
    (log/debug "[hardwallet] on card connected" data')
    (log/debug "[hardwallet] " (str "tag payload: " (clojure.string/join
                                                     (map js/String.fromCharCode payload))))
    (fx/merge cofx
              {:db                              (-> db
                                                    (assoc-in [:hardwallet :setup-step] :begin)
                                                    (assoc-in [:hardwallet :card-connected?] true))
               :hardwallet/get-application-info nil}
              (navigation/navigate-to-cofx :hardwallet-setup nil))))

(fx/defn on-card-disconnected
  [{:keys [db]} data]
  {:db (assoc-in db [:hardwallet :card-connected?] false)})

(fx/defn initialize-card
  [{:keys [db]}]
  {:hardwallet/initialize-card nil
   :db                         (assoc-in db [:hardwallet :setup-step] :preparing)})

(fx/defn on-initialization-success
  [{:keys [db]} secrets]
  (let [secrets' (js->clj secrets :keywordize-keys true)]
    {:db (-> db
             (assoc-in [:hardwallet :setup-step] :secret-keys)
             (assoc-in [:hardwallet :secrets] secrets'))}))

(fx/defn on-initialization-error
  [{:keys [db]} error]
  (log/debug "[hardwallet] initialization error: " error)
  {:db (-> db
           (assoc-in [:hardwallet :setup-step] :error)
           (assoc-in [:hardwallet :setup-error] error))})

(fx/defn on-pairing-success
  [{:keys [db]} pairing]
  {:db (-> db
           (assoc-in [:hardwallet :setup-step] :card-ready)
           (assoc-in [:hardwallet :secrets :pairing] pairing))})

(fx/defn on-pairing-error
  [{:keys [db]} error]
  (log/debug "[hardwallet] pairing error: " error)
  {:db (-> db
           (assoc-in [:hardwallet :setup-step] :error)
           (assoc-in [:hardwallet :setup-error] error))})

(fx/defn on-generate-mnemonic-success
  [{:keys [db]} mnemonic]
  {:db (-> db
           (assoc-in [:hardwallet :setup-step] :recovery-phrase)
           (assoc-in [:hardwallet :secrets :mnemonic] mnemonic))})

(fx/defn on-generate-mnemonic-error
  [{:keys [db]} error]
  (log/debug "[hardwallet] generate mnemonic error: " error)
  {:db (-> db
           (assoc-in [:hardwallet :setup-step] :error)
           (assoc-in [:hardwallet :setup-error] error))})

(fx/defn on-pin-validated [{:keys [db] :as cofx}]
  (let [pin (get-in db [:hardwallet :pin :original])
        password (apply str pin)]
    (fx/merge cofx
              ;{:hardwallet/create-account password}
)))

;; create account start

(defn create-account [password]
  (status/create-account
   password
   #(re-frame/dispatch [:hardwallet.callback/create-account-success (types/json->clj %) password])))

(fx/defn add-account
  "Takes db and new account, creates map of effects describing adding account to database and realm"
  [cofx {:keys [address] :as account}]
  (let [db (:db cofx)
        {:networks/keys [networks]} db
        enriched-account (assoc account
                                :network config/default-network
                                :networks networks
                                :address address)]
    {:db                 (assoc-in db [:accounts/accounts address] enriched-account)
     :data-store/base-tx [(accounts-store/save-account-tx enriched-account)]}))

(fx/defn on-account-created
  [{:keys [random-guid-generator
           signing-phrase
           status
           db] :as cofx}
   {:keys [pubkey address mnemonic]} password seed-backed-up]
  (let [normalized-address (utils.hex/normalize-hex address)
        account {:public-key             pubkey
                 :installation-id        (random-guid-generator)
                 :address                normalized-address
                 :name                   (gfycat/generate-gfy pubkey)
                 :status                 status
                 :signed-up?             true
                 :desktop-notifications? false
                 :photo-path             (identicon/identicon pubkey)
                 :signing-phrase         signing-phrase
                 :seed-backed-up?        seed-backed-up
                 :mnemonic               mnemonic
                 :settings               (constants/default-account-settings)}]
    (log/debug "account-created")
    (when-not (string/blank? pubkey)
      (fx/merge cofx
                {:db (assoc db :accounts/login {:address    normalized-address
                                                :password   password
                                                :processing true})}
                (add-account account)))))

(fx/defn on-create-account-success [{:keys [db] :as cofx} result password]
  (fx/merge cofx
            {:db (assoc-in db [:hardwallet :setup-step] :recovery-phrase)}
            (on-account-created result password false)))

;; create account finish

(fx/defn recovery-phrase-start-confirmation [{:keys [db]}]
  (let [mnemonic (get-in db [:hardwallet :secrets :mnemonic])
        [word1 word2] (shuffle (map-indexed vector (clojure.string/split mnemonic #" ")))
        word1 (zipmap [:idx :word] word1)
        word2 (zipmap [:idx :word] word2)]
    {:db (-> db
             (assoc-in [:hardwallet :setup-step] :recovery-phrase-confirm-word1)
             (assoc-in [:hardwallet :recovery-phrase :step] :word1)
             (assoc-in [:hardwallet :recovery-phrase :confirm-error] nil)
             (assoc-in [:hardwallet :recovery-phrase :input-word] nil)
             (assoc-in [:hardwallet :recovery-phrase :word1] word1)
             (assoc-in [:hardwallet :recovery-phrase :word2] word2))}))

(defn- show-recover-confirmation []
  {:ui/show-confirmation {:title               (i18n/label :t/are-you-sure?)
                          :content             (i18n/label :t/are-you-sure-description)
                          :confirm-button-text (clojure.string/upper-case (i18n/label :t/yes))
                          :cancel-button-text  (i18n/label :t/see-it-again)
                          :on-accept           #(re-frame/dispatch [:hardwallet.ui/recovery-phrase-confirm-pressed])
                          :on-cancel           #(re-frame/dispatch [:hardwallet.ui/recovery-phrase-cancel-pressed])}})

(defn- recovery-phrase-next-word [db]
  {:db (-> db
           (assoc-in [:hardwallet :recovery-phrase :step] :word2)
           (assoc-in [:hardwallet :recovery-phrase :confirm-error] nil)
           (assoc-in [:hardwallet :recovery-phrase :input-word] nil)
           (assoc-in [:hardwallet :setup-step] :recovery-phrase-confirm-word2))})

(fx/defn recovery-phrase-confirm-word
  [{:keys [db] :as cofx}]
  (let [step (get-in db [:hardwallet :recovery-phrase :step])
        input-word (get-in db [:hardwallet :recovery-phrase :input-word])
        {:keys [word]} (get-in db [:hardwallet :recovery-phrase step])]
    (if (= word input-word)
      (if (= step :word1)
        (recovery-phrase-next-word db)
        (show-recover-confirmation))
      {:db (assoc-in db [:hardwallet :recovery-phrase :confirm-error] (i18n/label :t/wrong-word))})))

(fx/defn on-mnemonic-confirmed
  [cofx]
  (fx/merge cofx
            {:hardwallet/save-mnemonic cofx}))

(fx/defn on-save-mnemonic-success
  [{:keys [db] :as cofx}]
  (fx/merge cofx
            {:db (-> db
                     (update-in [:hardwallet] dissoc :secrets)
                     (update-in [:hardwallet] dissoc :recovery-phrase))}
            (navigation/navigate-to-cofx :hardwallet-success nil)))

(fx/defn on-save-mnemonic-error
  [{:keys [db]} error]
  (log/debug "[hardwallet] save mnemonic error: " error)
  {:db (-> db
           (assoc-in [:hardwallet :setup-step] :error)
           (assoc-in [:hardwallet :setup-error] error))})

(re-frame/reg-fx
 :hardwallet/create-account
 create-account)
