(ns status-im.ui.screens.hardwallet.connect.styles
  (:require [status-im.ui.components.colors :as colors]))

(def container
  {:flex             1
   :background-color colors/white})

(def hardwallet-connect
  {:flex-direction  :column
   :flex            1
   :align-items     :center
   :justify-content :space-between})

(def hardwallet-card-image
  {:width  255
   :height 160})

(def turn-nfc-text-container
  {:margin-top 55})

(def status-hardwallet-text
  {:font-size   22
   :font-weight :bold
   :color       colors/black
   :text-align  :center})

(def link-card-text
  {:text-align         :center
   :font-size          15
   :color              colors/gray
   :padding-horizontal 80
   :padding-vertical   10})

(defn bottom-action-container [nfc-enabled?]
  {:background-color (if nfc-enabled?
                       colors/gray-background
                       colors/gray-lighter)
   :width            369
   :height           80
   :border-radius    10
   :margin-bottom    20})

(def phone-nfc-on-image
  {:width  301
   :height 205})

(def phone-nfc-off-image
  {:width  137
   :height 150})

(def hold-card-text
  {:width        186
   :text-align   :center
   :font-size    14
   :line-height  20
   :margin-right 40})

(def nfc-disabled-container
  {:flex-direction  :column
   :justify-content :space-between
   :align-items     :center
   :margin-top      100})

(def nfc-icon
  {:margin-left 52
   :margin-top  22})

(def turn-nfc-text
  {:text-transform :uppercase
   :line-height    20
   :letter-spacing 0.5
   :color          colors/gray})

(def go-to-settings-text
  {:text-align  :center
   :padding-top 9
   :color       colors/gray})

(def bottom-container
  {:height           52
   :justify-content  :center
   :border-top-width 1
   :border-color     colors/gray-light})

(def product-info-container
  {:flex-direction  :row
   :align-items     :center
   :justify-content :center})

(def product-info-text
  {:text-align :center
   :font-size  15
   :color      colors/blue})

(def external-link-icon
  {:margin-left 5})
