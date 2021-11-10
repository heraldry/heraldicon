(ns heraldry.coat-of-arms.ordinary.options
  (:require
   [heraldry.coat-of-arms.cottising :as cottising]
   [heraldry.coat-of-arms.geometry :as geometry]
   [heraldry.coat-of-arms.line.core :as line]
   [heraldry.coat-of-arms.line.fimbriation :as fimbriation]
   [heraldry.coat-of-arms.ordinary.interface :as ordinary-interface]
   [heraldry.coat-of-arms.ordinary.type.base :as base]
   [heraldry.coat-of-arms.ordinary.type.bend :as bend]
   [heraldry.coat-of-arms.ordinary.type.bend-sinister :as bend-sinister]
   [heraldry.coat-of-arms.ordinary.type.chevron :as chevron]
   [heraldry.coat-of-arms.ordinary.type.chief :as chief]
   [heraldry.coat-of-arms.ordinary.type.cross :as cross]
   [heraldry.coat-of-arms.ordinary.type.fess :as fess]
   [heraldry.coat-of-arms.ordinary.type.gore :as gore]
   [heraldry.coat-of-arms.ordinary.type.label :as label]
   [heraldry.coat-of-arms.ordinary.type.pale :as pale]
   [heraldry.coat-of-arms.ordinary.type.pall :as pall]
   [heraldry.coat-of-arms.ordinary.type.pile :as pile]
   [heraldry.coat-of-arms.ordinary.type.point :as point]
   [heraldry.coat-of-arms.ordinary.type.quarter :as quarter]
   [heraldry.coat-of-arms.ordinary.type.saltire :as saltire]
   [heraldry.coat-of-arms.position :as position]
   [heraldry.context :as c]
   [heraldry.interface :as interface]
   [heraldry.options :as options]
   [heraldry.strings :as strings]
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
   label/ordinary-type
   quarter/ordinary-type
   point/ordinary-type])

(def choices
  (->> ordinaries
       (map (fn [key]
              [(ordinary-interface/display-name key) key]))))

(def ordinary-map
  (util/choices->map choices))

(defn set-line-defaults [options]
  (-> options
      (options/override-if-exists [:fimbriation :alignment :default] :outside)))

(def type-option
  {:type :choice
   :choices choices
   :ui {:label strings/type
        :form-type :ordinary-type-select}})

(def default-options
  {:type type-option
   :origin (-> position/default-options
               (assoc-in [:ui :label] strings/origin))
   :direction-anchor (-> position/anchor-default-options
                         (dissoc :alignment)
                         (assoc-in [:angle :min] -180)
                         (assoc-in [:angle :max] 180)
                         (assoc-in [:angle :default] 0)
                         (assoc-in [:ui :label] strings/issuant))
   :anchor (-> position/anchor-default-options
               (assoc-in [:ui :label] strings/anchor))
   :line (set-line-defaults line/default-options)
   :opposite-line (-> (set-line-defaults line/default-options)
                      (assoc-in [:ui :label] strings/opposite-line))
   :extra-line (-> (set-line-defaults line/default-options)
                   (assoc-in [:ui :label] strings/extra-line))
   :geometry (-> geometry/default-options
                 (assoc-in [:size :min] 0.1)
                 (assoc-in [:size :max] 50)
                 (assoc-in [:size :default] 25)
                 (assoc :mirrored? nil)
                 (assoc :reversed? nil)
                 (assoc :stretch nil))
   :variant {:type :choice
             :choices [[{:en "Full"
                         :de "Durchgehend"} :full]
                       [{:en "Truncated"
                         :de "Schwebend"} :truncated]]
             :default :full
             :ui {:label strings/variant
                  :form-type :radio-select}}
   :num-points {:type :range
                :min 2
                :max 16
                :default 3
                :integer? true
                :ui {:label {:en "Number of points"
                             :de "Anzahl Lätze"}}}
   :outline? {:type :boolean
              :default false
              :ui {:label strings/outline}}
   :fimbriation fimbriation/default-options
   :cottising (-> cottising/default-options
                  (dissoc :cottise-extra-1)
                  (dissoc :cottise-extra-2))
   :manual-blazon {:type :text
                   :default nil
                   :ui {:label strings/manual-blazon}}})

(defn options [ordinary]
  (when ordinary
    (let [line-style (-> (line/options (:line ordinary))
                         set-line-defaults
                         (assoc :ui (-> default-options :line :ui)))
          sanitized-line (options/sanitize (:line ordinary) line-style)
          opposite-line-style (-> (line/options (:opposite-line ordinary) :inherited sanitized-line)
                                  set-line-defaults
                                  (assoc :ui (-> default-options :opposite-line :ui)))]
      (->
       (case (-> ordinary :type name keyword)
         :gore (options/pick default-options
                             [[:type]
                              [:origin]
                              [:anchor]
                              [:line]
                              [:opposite-line]
                              [:outline?]]
                             {[:line] (-> line-style
                                          (options/override-if-exists [:offset :min] 0)
                                          (options/override-if-exists [:base-line] nil)
                                          (options/override-if-exists [:type :default] :enarched)
                                          (options/override-if-exists [:flipped? :default] true))
                              [:opposite-line] (-> opposite-line-style
                                                   (options/override-if-exists [:offset :min] 0)
                                                   (options/override-if-exists [:base-line] nil)
                                                   (options/override-if-exists [:type :default] :enarched)
                                                   (options/override-if-exists [:flipped? :default] true))
                              [:origin :point :choices] (util/filter-choices
                                                         position/anchor-point-choices
                                                         [:fess :chief :base])
                              [:origin :alignment] nil
                              [:origin :point :default] :fess
                              [:anchor :point :choices] (util/filter-choices
                                                         position/anchor-point-choices
                                                         [:top-left :top-right :angle])
                              [:anchor :point :default] :top-left
                              [:anchor :angle :min] -80
                              [:anchor :angle :max] 80
                              [:anchor :alignment] nil})
         :label (options/pick default-options
                              [[:type]
                               [:origin]
                               [:geometry]
                               [:variant]
                               [:num-points]
                               [:outline?]
                               [:fimbriation]]
                              {[:origin :point :default] :chief
                               [:geometry :size :min] 2
                               [:geometry :size :default] 10
                               [:geometry :size :ui :label] strings/point-thickness
                               [:geometry :width] {:type :range
                                                   :min 10
                                                   :max 150
                                                   :default 66
                                                   :ui {:label strings/width
                                                        :step 0.1}}
                               [:geometry :thickness] {:type :range
                                                       :min 0
                                                       :max 20
                                                       :default 5
                                                       :ui {:label strings/bar-thickness
                                                            :step 0.1}}
                               [:geometry :eccentricity] {:type :range
                                                          :min 0
                                                          :max 1
                                                          :default 0
                                                          :ui {:label strings/eccentricity
                                                               :step 0.01}}
                               [:geometry :stretch] {:type :range
                                                     :min 0.33
                                                     :max 10
                                                     :default 2
                                                     :ui {:label strings/stretch
                                                          :step 0.01}}})
         :quarter (options/pick default-options
                                [[:type]
                                 [:origin]
                                 [:line]
                                 [:opposite-line]
                                 [:geometry]
                                 [:variant]
                                 [:outline?]
                                 [:cottising]]
                                {[:line] (-> line-style
                                             (options/override-if-exists [:offset :min] 0)
                                             (options/override-if-exists [:base-line] nil))
                                 [:opposite-line] (-> opposite-line-style
                                                      (options/override-if-exists [:offset :min] 0)
                                                      (options/override-if-exists [:base-line] nil))
                                 [:origin :point :default] :fess
                                 [:origin :alignment] nil
                                 [:geometry :size :min] 10
                                 [:geometry :size :max] 150
                                 [:geometry :size :default] 100
                                 [:variant :choices] [[{:en "Dexter-chief"
                                                        :de "Heraldisch rechts-oben"} :dexter-chief]
                                                      [{:en "Sinister-chief"
                                                        :de "Heraldisch links-oben"} :sinister-chief]
                                                      [{:en "Dexter-base"
                                                        :de "Heraldisch rechts-unten"} :dexter-base]
                                                      [{:en "Sinister-base"
                                                        :de "Heraldisch links-unten"} :sinister-base]]
                                 [:variant :default] :dexter-chief
                                 [:variant :ui :form-type] :select
                                 [:cottising] (-> default-options
                                                  :cottising
                                                  (dissoc :cottise-opposite-1)
                                                  (dissoc :cottise-opposite-2))})
         :point (options/pick default-options
                              [[:type]
                               [:line]
                               [:geometry]
                               [:variant]
                               [:outline?]
                               [:cottising]]
                              {[:line] (-> line-style
                                           (options/override-if-exists [:offset :min] 0)
                                           (options/override-if-exists [:base-line] nil))
                               [:variant :choices] [[strings/dexter :dexter]
                                                    [strings/sinister :sinister]]
                               [:variant :default] :dexter
                               [:geometry :size] nil
                               [:geometry :width] {:type :range
                                                   :min 10
                                                   :max 100
                                                   :default 50
                                                   :ui {:label strings/width}}
                               [:geometry :height] {:type :range
                                                    :min 10
                                                    :max 100
                                                    :default 50
                                                    :ui {:label strings/height}}
                               [:cottising] (-> default-options
                                                :cottising
                                                (dissoc :cottise-opposite-1)
                                                (dissoc :cottise-opposite-2))}))
       (assoc :manual-blazon (:manual-blazon default-options))
       (update :line (fn [line]
                       (when line
                         (set-line-defaults line))))
       (update :opposite-line (fn [opposite-line]
                                (when opposite-line
                                  (set-line-defaults opposite-line))))
       (update :extra-line (fn [extra-line]
                             (when extra-line
                               (set-line-defaults extra-line))))
       (update :origin (fn [origin]
                         (when origin
                           (position/adjust-options origin (-> ordinary :origin)))))
       (update :anchor (fn [anchor]
                         (when anchor
                           (position/adjust-options anchor (-> ordinary :anchor)))))
       (update :direction-anchor (fn [direction-anchor]
                                   (when direction-anchor
                                     (position/adjust-options direction-anchor (-> ordinary :direction-anchor)))))
       (update :fimbriation (fn [fimbriation]
                              (when fimbriation
                                (-> (fimbriation/options (:fimbriation ordinary)
                                                         :base-options (:fimbriation default-options))
                                    (assoc :ui {:label strings/fimbriation
                                                :form-type :fimbriation})))))
       (as-> options
         (cond-> options
           (:cottising options) (update :cottising cottising/options (:cottising ordinary))))))))

(defmethod interface/options-dispatch-fn :heraldry.component/ordinary [context]
  (interface/get-raw-data (c/++ context :type)))

(defmethod interface/component-options :heraldry.component/ordinary [context]
  (if (-> (interface/get-raw-data (c/++ context :type))
          name keyword
          #{:pale
            :fess
            :chief
            :base
            :bend
            :bend-sinister
            :chevron
            :pall
            :pile
            :saltire
            :cross})
    (-> (interface/options context)
        (assoc :type type-option)
        (assoc :manual-blazon options/manual-blazon))
    (options (interface/get-raw-data context))))
