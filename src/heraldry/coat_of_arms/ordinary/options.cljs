(ns heraldry.coat-of-arms.ordinary.options
  (:require [heraldry.coat-of-arms.geometry :as geometry]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.position :as position]
            [heraldry.coat-of-arms.shared.chevron :as chevron]
            [heraldry.util :as util]))

(defn set-line-defaults [options]
  (-> options
      (assoc-in [:fimbriation :alignment :default] :outside)))

(def default-options
  {:origin position/default-options
   :anchor position/anchor-default-options
   :variant {:type :choice
             :choices chevron/variant-choices
             :default :base}
   :line (set-line-defaults line/default-options)
   :opposite-line (set-line-defaults line/default-options)
   :geometry (-> geometry/default-options
                 (assoc-in [:size :min] 10)
                 (assoc-in [:size :max] 50)
                 (assoc-in [:size :default] 25)
                 (assoc :mirrored? nil)
                 (assoc :reversed? nil)
                 (assoc :stretch nil)
                 (assoc :rotation nil))})

(defn options [ordinary]
  (when ordinary
    (let [line-style (line/options (:line ordinary))]
      (->
       (case (:type ordinary)
         :pale (options/pick default-options
                             [[:origin]
                              [:line]
                              [:opposite-line]
                              [:geometry]]
                             {[:offset-y] nil
                              [:line] line-style})
         :fess (options/pick default-options
                             [[:origin]
                              [:line]
                              [:opposite-line]
                              [:geometry]]
                             {[:offset-x] nil
                              [:line] line-style})
         :chief (options/pick default-options
                              [[:line]
                               [:geometry]]
                              {[:line] line-style})
         :base (options/pick default-options
                             [[:line]
                              [:geometry]]
                             {[:line] line-style})
         :bend (options/pick default-options
                             [[:origin]
                              [:anchor]
                              [:line]
                              [:opposite-line]
                              [:geometry]]
                             (let [useful-points #{:top-left :bottom-right
                                                   :chief :honour :fess :nombril :base}
                                   point-choices (util/filter-choices
                                                  position/anchor-point-choices
                                                  useful-points)
                                   anchor-point-choices (util/filter-choices
                                                         position/anchor-point-choices
                                                         (conj useful-points :angle))]
                               {[:line] line-style
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
                                      [[:origin]
                                       [:anchor]
                                       [:line]
                                       [:opposite-line]
                                       [:geometry]]
                                      (let [useful-points #{:top-right :bottom-left
                                                            :chief :honour :fess :nombril :base}
                                            point-choices (util/filter-choices
                                                           position/anchor-point-choices
                                                           useful-points)
                                            anchor-point-choices (util/filter-choices
                                                                  position/anchor-point-choices
                                                                  (conj useful-points :angle))]
                                        {[:line] line-style
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
                                [[:variant]
                                 [:origin]
                                 [:anchor]
                                 [:line]
                                 [:opposite-line]
                                 [:geometry]]
                                {[:line] (-> line-style
                                             (options/override-if-exists [:offset :min] 0))
                                 [:opposite-line] (-> line-style
                                                      (options/override-if-exists [:offset :min] 0))
                                 [:anchor :point :choices] (case (-> ordinary :variant (or :base))
                                                             :chief (util/filter-choices
                                                                     position/anchor-point-choices
                                                                     [:top-left :top-right :angle])
                                                             :dexter (util/filter-choices
                                                                      position/anchor-point-choices
                                                                      [:top-left :bottom-left :angle])
                                                             :sinister (util/filter-choices
                                                                        position/anchor-point-choices
                                                                        [:top-right :bottom-right :angle])
                                                                   ;; otherwise, assume :base
                                                             (util/filter-choices
                                                              position/anchor-point-choices
                                                              [:bottom-left :bottom-right :angle]))})
         :pile (options/pick default-options
                             [[:origin]
                              [:anchor]
                              [:line]
                              [:opposite-line]
                              [:geometry]]
                             (let [anchor-points #{:top-left :top :top-right
                                                   :left :right
                                                   :bottom-left :bottom :bottom-right
                                                   :fess :honour :nombril :base :chief
                                                   :angle}]
                               {[:line] (-> line-style
                                            (options/override-if-exists [:offset :min] 0))
                                [:opposite-line] (-> line-style
                                                     (options/override-if-exists [:offset :min] 0))
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
                                                   :default :edge})}))
         ;; TODO: perhaps there should be origin options for the corners?
         ;; so one can align fro top-left to bottom-right
         :saltire (options/pick default-options
                                [[:origin]
                                 [:anchor]
                                 [:line]
                                 [:geometry]]
                                {[:line] (-> line-style
                                             (options/override-if-exists [:offset :min] 0))
                                 [:opposite-line] (-> line-style
                                                      (options/override-if-exists [:offset :min] 0))
                                 [:origin :alignment] nil
                                 [:anchor :point :choices] (util/filter-choices
                                                            position/anchor-point-choices
                                                            [:top-left :top-right :bottom-left :bottom-right :angle])})
         :cross (options/pick default-options
                              [[:origin]
                               [:line]
                               [:geometry]]
                              {[:line] (-> line-style
                                           (options/override-if-exists [:offset :min] 0))
                               [:origin :alignment] nil}))
       (update-in [:line] (fn [line]
                            (when line
                              (set-line-defaults line))))
       (update-in [:opposite-line] (fn [opposite-line]
                                     (when opposite-line
                                       (set-line-defaults opposite-line))))
       (update-in [:anchor] (fn [anchor]
                              (when anchor
                                (position/adjust-options anchor (-> ordinary :anchor)))))))))

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
                         (:flipped? line)))))
