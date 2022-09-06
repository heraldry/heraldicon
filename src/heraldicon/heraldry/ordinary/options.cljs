(ns heraldicon.heraldry.ordinary.options
  (:require
   [heraldicon.heraldry.ordinary.interface :as ordinary.interface]
   [heraldicon.heraldry.ordinary.type.base :as base]
   [heraldicon.heraldry.ordinary.type.bend :as bend]
   [heraldicon.heraldry.ordinary.type.bend-sinister :as bend-sinister]
   [heraldicon.heraldry.ordinary.type.bordure :as bordure]
   [heraldicon.heraldry.ordinary.type.chevron :as chevron]
   [heraldicon.heraldry.ordinary.type.chief :as chief]
   [heraldicon.heraldry.ordinary.type.cross :as cross]
   [heraldicon.heraldry.ordinary.type.fess :as fess]
   [heraldicon.heraldry.ordinary.type.gore :as gore]
   [heraldicon.heraldry.ordinary.type.label :as label]
   [heraldicon.heraldry.ordinary.type.orle :as orle]
   [heraldicon.heraldry.ordinary.type.pale :as pale]
   [heraldicon.heraldry.ordinary.type.pall :as pall]
   [heraldicon.heraldry.ordinary.type.pile :as pile]
   [heraldicon.heraldry.ordinary.type.point :as point]
   [heraldicon.heraldry.ordinary.type.quarter :as quarter]
   [heraldicon.heraldry.ordinary.type.saltire :as saltire]
   [heraldicon.interface :as interface]
   [heraldicon.options :as options]))

(derive :heraldry.ordinary/type :heraldry/ordinary)

(def ^:private ordinaries
  [fess/ordinary-type
   pale/ordinary-type
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
              (derive key :heraldry.ordinary/type)
              [(ordinary.interface/display-name key) key]))
       vec))

(def ordinary-map
  (options/choices->map choices))

(def ^:private type-option
  {:type :option.type/choice
   :choices choices
   :ui/label :string.option/type
   :ui/element :ui.element/ordinary-type-select})

(derive :heraldry/ordinary :heraldry.options/root)

(defmethod interface/options :heraldry/ordinary [context]
  (-> context
      ordinary.interface/options
      (assoc :type type-option)
      (assoc :manual-blazon options/manual-blazon)))
