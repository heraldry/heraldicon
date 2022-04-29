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
   [heraldicon.context :as c]
   [heraldicon.interface :as interface]
   [heraldicon.options :as options]
   [heraldicon.util :as util]))

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
              [(ordinary.interface/display-name key) key]))))

(def ordinary-map
  (util/choices->map choices))

(def type-option
  {:type :choice
   :choices choices
   :ui {:label :string.option/type
        :form-type :ordinary-type-select}})

(defmethod interface/options-subscriptions :heraldry.component/ordinary [_context]
  #{[:type]
    [:line]
    [:line :type]
    [:line :fimbriation]
    [:line :fimbriation :mode]
    [:opposite-line :type]
    [:opposite-line :fimbriation :mode]
    [:extra-line :type]
    [:extra-line :fimbriation :mode]

    [:cottising :cottise-1 :line]
    [:cottising :cottise-1 :line :type]
    [:cottising :cottise-1 :line :fimbriation]
    [:cottising :cottise-1 :line :fimbriation :mode]
    [:cottising :cottise-1 :opposite-line :type]
    [:cottising :cottise-1 :opposite-line :fimbriation :mode]
    [:cottising :cottise-1 :extra-line :type]
    [:cottising :cottise-1 :extra-line :fimbriation :mode]

    [:cottising :cottise-2 :line]
    [:cottising :cottise-2 :line :type]
    [:cottising :cottise-2 :line :fimbriation]
    [:cottising :cottise-2 :line :fimbriation :mode]
    [:cottising :cottise-2 :opposite-line :type]
    [:cottising :cottise-2 :opposite-line :fimbriation :mode]
    [:cottising :cottise-2 :extra-line :type]
    [:cottising :cottise-2 :extra-line :fimbriation :mode]

    [:cottising :cottise-opposite-1 :line]
    [:cottising :cottise-opposite-1 :line :type]
    [:cottising :cottise-opposite-1 :line :fimbriation]
    [:cottising :cottise-opposite-1 :line :fimbriation :mode]
    [:cottising :cottise-opposite-1 :opposite-line :type]
    [:cottising :cottise-opposite-1 :opposite-line :fimbriation :mode]
    [:cottising :cottise-opposite-1 :extra-line :type]
    [:cottising :cottise-opposite-1 :extra-line :fimbriation :mode]

    [:cottising :cottise-opposite-2 :line]
    [:cottising :cottise-opposite-2 :line :type]
    [:cottising :cottise-opposite-2 :line :fimbriation]
    [:cottising :cottise-opposite-2 :line :fimbriation :mode]
    [:cottising :cottise-opposite-2 :opposite-line :type]
    [:cottising :cottise-opposite-2 :opposite-line :fimbriation :mode]
    [:cottising :cottise-opposite-2 :extra-line :type]
    [:cottising :cottise-opposite-2 :extra-line :fimbriation :mode]

    [:cottising :cottise-extra-1 :line]
    [:cottising :cottise-extra-1 :line :type]
    [:cottising :cottise-extra-1 :line :fimbriation]
    [:cottising :cottise-extra-1 :line :fimbriation :mode]
    [:cottising :cottise-extra-1 :opposite-line :type]
    [:cottising :cottise-extra-1 :opposite-line :fimbriation :mode]
    [:cottising :cottise-extra-1 :extra-line :type]
    [:cottising :cottise-extra-1 :extra-line :fimbriation :mode]

    [:cottising :cottise-extra-2 :line]
    [:cottising :cottise-extra-2 :line :type]
    [:cottising :cottise-extra-2 :line :fimbriation]
    [:cottising :cottise-extra-2 :line :fimbriation :mode]
    [:cottising :cottise-extra-2 :opposite-line :type]
    [:cottising :cottise-extra-2 :opposite-line :fimbriation :mode]
    [:cottising :cottise-extra-2 :extra-line :type]
    [:cottising :cottise-extra-2 :extra-line :fimbriation :mode]

    [:anchor :point]
    [:origin :point]
    [:orientation :point]

    [:fimbriation :mode]
    [:geometry :size-mode]
    [:distance]
    [:thickness]
    [:voided :voided?]
    [:humetty :humetty?]

    [:num-points]})

(defmethod interface/options :heraldry.component/ordinary [context]
  (-> context
      (assoc :dispatch-value (interface/get-raw-data (c/++ context :type)))
      interface/options
      (assoc :type type-option)
      (assoc :manual-blazon options/manual-blazon)))
