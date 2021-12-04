(ns heraldry.coat-of-arms.ordinary.options
  (:require
   [heraldry.coat-of-arms.ordinary.interface :as ordinary-interface]
   [heraldry.coat-of-arms.ordinary.type.base :as base]
   [heraldry.coat-of-arms.ordinary.type.bend :as bend]
   [heraldry.coat-of-arms.ordinary.type.bend-sinister :as bend-sinister]
   [heraldry.coat-of-arms.ordinary.type.bordure :as bordure]
   [heraldry.coat-of-arms.ordinary.type.chevron :as chevron]
   [heraldry.coat-of-arms.ordinary.type.chief :as chief]
   [heraldry.coat-of-arms.ordinary.type.cross :as cross]
   [heraldry.coat-of-arms.ordinary.type.fess :as fess]
   [heraldry.coat-of-arms.ordinary.type.gore :as gore]
   [heraldry.coat-of-arms.ordinary.type.label :as label]
   [heraldry.coat-of-arms.ordinary.type.orle :as orle]
   [heraldry.coat-of-arms.ordinary.type.pale :as pale]
   [heraldry.coat-of-arms.ordinary.type.pall :as pall]
   [heraldry.coat-of-arms.ordinary.type.pile :as pile]
   [heraldry.coat-of-arms.ordinary.type.point :as point]
   [heraldry.coat-of-arms.ordinary.type.quarter :as quarter]
   [heraldry.coat-of-arms.ordinary.type.saltire :as saltire]
   [heraldry.context :as c]
   [heraldry.gettext :refer [string]]
   [heraldry.interface :as interface]
   [heraldry.options :as options]
   [heraldry.util :as util]))

(def ordinaries
  [pale/ordinary-type
   fess/ordinary-type
   chief/ordinary-type
   base/ordinary-type
   bend/ordinary-type
   bend-sinister/ordinary-type
   cross/ordinary-type
   saltire/ordinary-type
   chevron/ordinary-type
   pall/ordinary-type
   pile/ordinary-type
   gore/ordinary-type
   quarter/ordinary-type
   point/ordinary-type
   bordure/ordinary-type
   orle/ordinary-type
   label/ordinary-type])

(def choices
  (->> ordinaries
       (map (fn [key]
              [(ordinary-interface/display-name key) key]))))

(def ordinary-map
  (util/choices->map choices))

(def type-option
  {:type :choice
   :choices choices
   :ui {:label (string "Type")
        :form-type :ordinary-type-select}})

(defmethod interface/options-subscriptions :heraldry.component/ordinary [_context]
  options/shared-options-subscriptions)

(defmethod interface/options :heraldry.component/ordinary [context]
  (-> context
      (assoc :dispatch-value (interface/get-raw-data (c/++ context :type)))
      interface/options
      (assoc :type type-option)
      (assoc :manual-blazon options/manual-blazon)))
