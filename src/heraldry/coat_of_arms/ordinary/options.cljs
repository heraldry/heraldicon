(ns heraldry.coat-of-arms.ordinary.options
  (:require [heraldry.coat-of-arms.cottising :as cottising]
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

(def default-options
  {:type {:type :choice
          :choices choices
          :ui {:label strings/variant
               :form-type :ordinary-type-select}}
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
             :choices [["Full" :full]
                       ["Truncated" :truncated]]
             :default :full
             :ui {:label strings/variant
                  :form-type :radio-select}}
   :num-points {:type :range
                :min 2
                :max 16
                :default 3
                :integer? true
                :ui {:label "Points"}}
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
                                  (assoc :ui (-> default-options :opposite-line :ui)))
          extra-line-style (-> (line/options (:extra-line ordinary) :inherited sanitized-line)
                               set-line-defaults
                               (assoc :ui (-> default-options :extra-line :ui)))]
      (->
       (case (-> ordinary :type name keyword)
         :pale (options/pick default-options
                             [[:type]
                              [:origin]
                              [:line]
                              [:opposite-line]
                              [:geometry]
                              [:outline?]
                              [:cottising]]
                             {[:offset-y] nil
                              [:line] line-style
                              [:opposite-line] opposite-line-style})
         :fess (options/pick default-options
                             [[:type]
                              [:origin]
                              [:line]
                              [:opposite-line]
                              [:geometry]
                              [:outline?]
                              [:cottising]]
                             {[:offset-x] nil
                              [:line] line-style
                              [:opposite-line] opposite-line-style})
         :chief (options/pick default-options
                              [[:type]
                               [:line]
                               [:geometry]
                               [:outline?]]
                              {[:line] line-style
                               [:cottising] (-> default-options
                                                :cottising
                                                (dissoc :cottise-opposite-1)
                                                (dissoc :cottise-opposite-2))})
         :base (options/pick default-options
                             [[:type]
                              [:line]
                              [:geometry]
                              [:outline?]]
                             {[:line] line-style
                              [:cottising] (-> default-options
                                               :cottising
                                               (dissoc :cottise-opposite-1)
                                               (dissoc :cottise-opposite-2))})
         :bend (options/pick default-options
                             [[:type]
                              [:origin]
                              [:anchor]
                              [:line]
                              [:opposite-line]
                              [:geometry]
                              [:outline?]
                              [:cottising]]
                             (let [useful-points #{:top-left :bottom-right
                                                   :chief :honour :fess :nombril :base}
                                   point-choices (util/filter-choices
                                                  position/anchor-point-choices
                                                  useful-points)
                                   anchor-point-choices (util/filter-choices
                                                         position/anchor-point-choices
                                                         (conj useful-points :angle))]
                               {[:line] line-style
                                [:opposite-line] opposite-line-style
                                [:origin :point :choices] point-choices
                                [:origin :point :default] :top-left
                                [:anchor :point :choices] (case (-> ordinary :origin :point (or :top-left))
                                                            :top-left (util/filter-choices
                                                                       anchor-point-choices
                                                                       #{:bottom-right
                                                                         :chief :honour :fess :nombril :base :angle})
                                                            :bottom-right (util/filter-choices
                                                                           anchor-point-choices
                                                                           #{:top-left
                                                                             :chief :honour :fess :nombril :base :angle})
                                                            (util/filter-choices
                                                             anchor-point-choices
                                                             [:top-left :bottom-right :angle]))
                                [:anchor :point :default] (case (-> ordinary :origin :point (or :top-left))
                                                            :top-left :fess
                                                            :bottom-right :fess
                                                            :top-left)}))
         :bend-sinister (options/pick default-options
                                      [[:type]
                                       [:origin]
                                       [:anchor]
                                       [:line]
                                       [:opposite-line]
                                       [:geometry]
                                       [:outline?]
                                       [:cottising]]
                                      (let [useful-points #{:top-right :bottom-left
                                                            :chief :honour :fess :nombril :base}
                                            point-choices (util/filter-choices
                                                           position/anchor-point-choices
                                                           useful-points)
                                            anchor-point-choices (util/filter-choices
                                                                  position/anchor-point-choices
                                                                  (conj useful-points :angle))]
                                        {[:line] line-style
                                         [:opposite-line] opposite-line-style
                                         [:origin :point :choices] point-choices
                                         [:origin :point :default] :top-left
                                         [:anchor :point :choices] (case (-> ordinary :origin :point (or :top-right))
                                                                     :top-right (util/filter-choices
                                                                                 anchor-point-choices
                                                                                 #{:bottom-left
                                                                                   :chief :honour :fess :nombril :base :angle})
                                                                     :bottom-left (util/filter-choices
                                                                                   anchor-point-choices
                                                                                   #{:top-right
                                                                                     :chief :honour :fess :nombril :base :angle})
                                                                     (util/filter-choices
                                                                      anchor-point-choices
                                                                      [:top-right :bottom-left :angle]))
                                         [:anchor :point :default] (case (-> ordinary :origin :point (or :top-right))
                                                                     :top-right :fess
                                                                     :bottom-left :fess
                                                                     :top-right)}))
         :chevron (options/pick default-options
                                [[:type]
                                 [:origin]
                                 [:direction-anchor]
                                 [:anchor]
                                 [:line]
                                 [:opposite-line]
                                 [:geometry]
                                 [:outline?]
                                 [:cottising]]
                                {[:line] (-> line-style
                                             (options/override-if-exists [:offset :min] 0)
                                             (options/override-if-exists [:base-line] nil))
                                 [:opposite-line] (-> opposite-line-style
                                                      (options/override-if-exists [:offset :min] 0)
                                                      (options/override-if-exists [:base-line] nil))
                                 [:direction-anchor :point :choices] (util/filter-choices
                                                                      position/anchor-point-choices
                                                                      [:top-left :top :top-right :left :right :bottom-left :bottom :bottom-right :angle])
                                 [:direction-anchor :point :default] :bottom
                                 [:anchor :point :choices] (util/filter-choices
                                                            position/anchor-point-choices
                                                            (case (-> ordinary :direction-anchor :point (or :bottom))
                                                              :bottom [:bottom-left :bottom :bottom-right :left :right :angle]
                                                              :top [:top-left :top :top-right :left :right :angle]
                                                              :left [:top-left :left :bottom-left :top :bottom :angle]
                                                              :right [:top-right :right :bottom-right :top :bottom :angle]
                                                              :bottom-left [:bottom-left :bottom :bottom-right :top-left :left :angle]
                                                              :bottom-right [:bottom-left :bottom :bottom-right :right :top-right :angle]
                                                              :top-left [:top-left :top :top-right :left :bottom-left :angle]
                                                              :top-right [:top-left :top :top-right :left :bottom-right :angle]
                                                              [:top-left :top :top-right :left :right :bottom-left :bottom :bottom-right :angle]))
                                 [:anchor :point :default] (case (-> ordinary :direction-anchor :point (or :bottom))
                                                             :bottom :bottom-left
                                                             :top :top-right
                                                             :left :top-left
                                                             :right :bottom-right
                                                             :bottom-left :left
                                                             :bottom-right :bottom
                                                             :top-left :top
                                                             :top-right :right
                                                             :angle :angle
                                                             :bottom-left)})
         :pall (options/pick default-options
                             [[:type]
                              [:origin]
                              [:direction-anchor]
                              [:anchor]
                              [:line]
                              [:opposite-line]
                              [:extra-line]
                              [:geometry]
                              [:outline?]
                              [:cottising]]
                             {[:line] (-> line-style
                                          (options/override-if-exists [:offset :min] 0)
                                          (options/override-if-exists [:base-line] nil))
                              [:opposite-line] (-> opposite-line-style
                                                   (options/override-if-exists [:offset :min] 0)
                                                   (options/override-if-exists [:base-line] nil))
                              [:extra-line] (-> extra-line-style
                                                (options/override-if-exists [:offset :min] 0)
                                                (options/override-if-exists [:base-line] nil))
                              [:cottising] cottising/default-options
                              [:direction-anchor :point :choices] (util/filter-choices
                                                                   position/anchor-point-choices
                                                                   [:top-left :top :top-right :left :right :bottom-left :bottom :bottom-right :angle])
                              [:direction-anchor :point :default] :top
                              [:anchor :point :choices] (util/filter-choices
                                                         position/anchor-point-choices
                                                         (case (-> ordinary :direction-anchor :point (or :top))
                                                           :bottom [:bottom-left :bottom :bottom-right :left :right :angle]
                                                           :top [:top-left :top :top-right :left :right :angle]
                                                           :left [:top-left :left :bottom-left :top :bottom :angle]
                                                           :right [:top-right :right :bottom-right :top :bottom :angle]
                                                           :bottom-left [:bottom-left :bottom :bottom-right :top-left :left :angle]
                                                           :bottom-right [:bottom-left :bottom :bottom-right :right :top-right :angle]
                                                           :top-left [:top-left :top :top-right :left :bottom-left :angle]
                                                           :top-right [:top-left :top :top-right :left :bottom-right :angle]
                                                           [:top-left :top :top-right :left :right :bottom-left :bottom :bottom-right :angle]))
                              [:anchor :point :default] (case (-> ordinary :direction-anchor :point (or :top))
                                                          :bottom :bottom-left
                                                          :top :top-right
                                                          :left :top-left
                                                          :right :bottom-right
                                                          :bottom-left :left
                                                          :bottom-right :bottom
                                                          :top-left :top
                                                          :top-right :right
                                                          :angle :angle
                                                          :bottom-left)
                              [:geometry :size :default] 20})
         :pile (options/pick default-options
                             [[:type]
                              [:origin]
                              [:anchor]
                              [:line]
                              [:opposite-line]
                              [:geometry]
                              [:outline?]
                              [:cottising]]
                             (let [anchor-points #{:top-left :top :top-right
                                                   :left :right
                                                   :bottom-left :bottom :bottom-right
                                                   :fess :honour :nombril :base :chief
                                                   :angle}]
                               {[:line] (-> line-style
                                            (options/override-if-exists [:offset :min] 0)
                                            (options/override-if-exists [:base-line] nil))
                                [:opposite-line] (-> opposite-line-style
                                                     (options/override-if-exists [:offset :min] 0)
                                                     (options/override-if-exists [:base-line] nil))
                                [:geometry :size-mode] {:type :choice
                                                        :choices [[strings/thickness :thickness]
                                                                  [strings/angle :angle]]
                                                        :default :thickness
                                                        :ui {:label {:en "Size mode"
                                                                     :de "Größenmodus"}
                                                             :form-type :radio-select}}
                                [:geometry :size :min] 5
                                [:geometry :size :max] 100
                                [:geometry :size :default] (case (-> ordinary :geometry :size-mode (or :thickness))
                                                             :thickness 75
                                                             30)
                                [:geometry :stretch] {:type :range
                                                      :min 0.33
                                                      :max 2
                                                      :default 1
                                                      :ui {:label strings/stretch
                                                           :step 0.01}}
                                [:origin :point :choices] (util/filter-choices
                                                           position/anchor-point-choices
                                                           anchor-points)
                                [:origin :point :default] :top
                                [:anchor :point :choices] (util/filter-choices
                                                           position/anchor-point-choices
                                                           (disj anchor-points (-> ordinary :origin :point (or :top))))
                                [:anchor :point :default] :fess
                                [:anchor :alignment] nil
                                [:anchor :angle :default] (cond
                                                            (#{:top-left
                                                               :top-right
                                                               :bottom-left
                                                               :bottom-right} (-> ordinary :origin :point (or :top))) 45
                                                            :else 0)
                                [:anchor :angle :min] (cond
                                                        (#{:top-left
                                                           :top-right
                                                           :bottom-left
                                                           :bottom-right} (-> ordinary :origin :point (or :top))) 0
                                                        :else -90)
                                [:anchor :angle :max] 90
                                [:anchor :type] (when (-> ordinary :anchor :point (not= :angle))
                                                  {:type :choice
                                                   :choices [["Edge" :edge]
                                                             ["Point" :point]]
                                                   :default :edge
                                                   :ui {:label "Point mode"
                                                        :form-type :radio-select}})
                                [:cottising] (-> default-options
                                                 :cottising
                                                 (dissoc :cottise-opposite-1)
                                                 (dissoc :cottise-opposite-2))}))
         ;; TODO: perhaps there should be origin options for the corners?
         ;; so one can align fro top-left to bottom-right
         :saltire (options/pick default-options
                                [[:type]
                                 [:origin]
                                 [:anchor]
                                 [:line]
                                 [:geometry]
                                 [:outline?]
                                 [:cottising]]
                                {[:line] (-> line-style
                                             (options/override-if-exists [:offset :min] 0)
                                             (options/override-if-exists [:base-line] nil))
                                 [:origin :alignment] nil
                                 [:anchor :point :choices] (util/filter-choices
                                                            position/anchor-point-choices
                                                            [:top-left :top-right :bottom-left :bottom-right :angle])
                                 [:cottising] (-> default-options
                                                  :cottising
                                                  (dissoc :cottise-opposite-1)
                                                  (dissoc :cottise-opposite-2))})
         :cross (options/pick default-options
                              [[:type]
                               [:origin]
                               [:line]
                               [:geometry]
                               [:outline?]
                               [:cottising]]
                              {[:line] (-> line-style
                                           (options/override-if-exists [:offset :min] 0)
                                           (options/override-if-exists [:base-line] nil))
                               [:origin :alignment] nil
                               [:cottising] (-> default-options
                                                :cottising
                                                (dissoc :cottise-opposite-1)
                                                (dissoc :cottise-opposite-2))})
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
                               [:geometry :size :ui :label] "Point thickness"
                               [:geometry :width] {:type :range
                                                   :min 10
                                                   :max 150
                                                   :default 66
                                                   :ui {:label "Width"
                                                        :step 0.1}}
                               [:geometry :thickness] {:type :range
                                                       :min 0
                                                       :max 20
                                                       :default 5
                                                       :ui {:label "Bar thickness"
                                                            :step 0.1}}
                               [:geometry :eccentricity] {:type :range
                                                          :min 0
                                                          :max 1
                                                          :default 0
                                                          :ui {:label "Eccentricity"
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
                                 [:variant :choices] [["Dexter-chief" :dexter-chief]
                                                      ["Sinister-chief" :sinister-chief]
                                                      ["Dexter-base" :dexter-base]
                                                      ["Sinister-base" :sinister-base]]
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
                               [:variant :choices] [["Dexter" :dexter]
                                                    ["Sinister" :sinister]]
                               [:variant :default] :dexter
                               [:geometry :size] nil
                               [:geometry :width] {:type :range
                                                   :min 10
                                                   :max 100
                                                   :default 50
                                                   :ui {:label "Width"}}
                               [:geometry :height] {:type :range
                                                    :min 10
                                                    :max 100
                                                    :default 50
                                                    :ui {:label "Height"}}
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

(defmethod interface/component-options :heraldry.component/ordinary [_path data]
  (options data))
