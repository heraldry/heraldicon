(ns heraldry.coat-of-arms.ordinary.options
  (:require [heraldry.coat-of-arms.cottising :as cottising]
            [heraldry.coat-of-arms.geometry :as geometry]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.line.fimbriation :as fimbriation]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.position :as position]
            [heraldry.util :as util]))

(defn set-line-defaults [options]
  (-> options
      (assoc-in [:fimbriation :alignment :default] :outside)))

(def default-options
  {:type {:type :choice
          ;; TODO: not ideal, but currently would create a circular dependency
          :choices []
          :default :heraldry.ordinary.type/pale
          :ui {:label "Type"
               :form-type :ordinary-type-select}}
   :origin (-> position/default-options
               (assoc-in [:ui :label] "Origin"))
   :direction-anchor (-> position/anchor-default-options
                         (dissoc :alignment)
                         (assoc-in [:angle :min] -180)
                         (assoc-in [:angle :max] 180)
                         (assoc-in [:angle :default] 0)
                         (assoc-in [:ui :label] "Issuant"))
   :anchor (-> position/anchor-default-options
               (assoc-in [:ui :label] "Anchor"))
   :line (set-line-defaults line/default-options)
   :opposite-line (-> (set-line-defaults line/default-options)
                      (assoc-in [:ui :label] "Opposite line"))
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
             :ui {:label "Variant"
                  :form-type :radio-select}}
   :num-points {:type :range
                :min 2
                :max 16
                :default 3
                :integer? true
                :ui {:label "Points"}}
   :outline? {:type :boolean
              :default false
              :ui {:label "Outline"}}
   :fimbriation fimbriation/default-options
   :cottising cottising/options})

(defn options [ordinary]
  (when ordinary
    (let [line-style (-> (line/options (:line ordinary))
                         (assoc :ui (-> default-options :line :ui)))
          opposite-line-style (-> (line/options {:type (-> ordinary :opposite-line :type (or (-> ordinary :line :type)))})
                                  (assoc :ui (-> default-options :opposite-line :ui)))]
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
                                                        :choices [["Thickness" :thickness]
                                                                  ["Angle" :angle]]
                                                        :default :thickness}
                                [:geometry :size :min] 5
                                [:geometry :size :max] 100
                                [:geometry :size :default] (case (-> ordinary :geometry :size-mode (or :thickness))
                                                             :thickness 75
                                                             30)
                                [:geometry :stretch] {:type :range
                                                      :min 0.33
                                                      :max 2
                                                      :default 1}
                                [:origin :point :choices] (util/filter-choices
                                                           position/anchor-point-choices
                                                           [:top-left :top :top-right
                                                            :left :right
                                                            :bottom-left :bottom :bottom-right])
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
                                                   :default :edge})
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
                                                     :ui {:label "Stretch"
                                                          :step 0.01}}}))
       (update :line (fn [line]
                       (when line
                         (set-line-defaults line))))
       (update :opposite-line (fn [opposite-line]
                                (when opposite-line
                                  (set-line-defaults opposite-line))))
       (update :anchor (fn [anchor]
                         (when anchor
                           (position/adjust-options anchor (-> ordinary :anchor)))))
       (update :direction-anchor (fn [direction-anchor]
                                   (when direction-anchor
                                     (position/adjust-options direction-anchor (-> ordinary :direction-anchor)))))
       (update :fimbriation (fn [fimbriation]
                              (when fimbriation
                                (-> (fimbriation/options (:fimbriation ordinary))
                                    (assoc :ui {:label "Fimbriation"
                                                :form-type :fimbriation})))))))))

(defn sanitize-opposite-line [ordinary line]
  (-> (options/sanitize
       (util/deep-merge-with (fn [_current-value new-value]
                               new-value) line
                             (into {}
                                   (filter (fn [[_ v]]
                                             (some? v))
                                           (:opposite-line ordinary))))
       (-> ordinary options :opposite-line))
      (assoc :flipped? (if (-> ordinary :opposite-line :flipped?)
                         (not (:flipped? line))
                         (:flipped? line)))
      (assoc :mirrored? (if (-> ordinary :opposite-line :mirrored?)
                          (not (:mirrored? line))
                          (:mirrored? line)))))
