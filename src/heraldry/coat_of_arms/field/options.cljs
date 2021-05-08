(ns heraldry.coat-of-arms.field.options
  (:require [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.position :as position]
            [heraldry.util :as util]))

(def default-options
  {:line line/default-options
   :opposite-line line/default-options
   :extra-line line/default-options
   :origin (-> position/default-options
               (dissoc :alignment))
   :direction-anchor (-> position/anchor-default-options
                         (dissoc :alignment)
                         (assoc-in [:angle :min] -180)
                         (assoc-in [:angle :max] 180)
                         (assoc-in [:angle :default] 0))
   :anchor (-> position/anchor-default-options
               (dissoc :alignment))
   :variant {:type :choice
             :choices [["Default" :default]
                       ["Counter" :counter]
                       ["In pale" :in-pale]
                       ["En point" :en-point]
                       ["Ancien" :ancien]]
             :default :default}
   :thickness {:type :range
               :min 0
               :max 0.5
               :default 0.1}
   :layout {:num-fields-x {:type :range
                           :min 1
                           :max 20
                           :default 6
                           :integer? true}
            :num-fields-y {:type :range
                           :min 1
                           :max 20
                           :default 6
                           :integer? true}
            :num-base-fields {:type :range
                              :min 2
                              :max 8
                              :default 2
                              :integer? true}
            :offset-x {:type :range
                       :min -1
                       :max 1
                       :default 0}
            :offset-y {:type :range
                       :min -1
                       :max 1
                       :default 0}
            :stretch-x {:type :range
                        :min 0.5
                        :max 2
                        :default 1}
            :stretch-y {:type :range
                        :min 0.5
                        :max 2
                        :default 1}
            :rotation {:type :range
                       :min -90
                       :max 90
                       :default 0}}})

(defn options [field]
  (when field
    (let [line-style (line/options (:line field))
          opposite-line-style (line/options {:type (-> field :opposite-line :type (or (-> field :line :type)))})
          extra-line-style (line/options {:type (-> field :extra-line :type (or (-> field :line :type)))})]
      (-> (case (-> field :type name keyword)
            :per-pale (options/pick default-options
                                    [[:line]
                                     [:origin :point]
                                     [:origin :offset-x]]
                                    {[:origin :point :choices] position/point-choices-x
                                     [:line] line-style})
            :per-fess (options/pick default-options
                                    [[:line]
                                     [:origin :point]
                                     [:origin :offset-y]]
                                    {[:origin :point :choices] position/point-choices-y
                                     [:line] line-style})
            :per-bend (options/pick default-options
                                    [[:line]
                                     [:origin]
                                     [:anchor]]
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
                                       [:anchor :point :choices] (case (-> field :origin :point (or :top-left))
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
                                       [:anchor :point :default] (case (-> field :origin :point (or :top-left))
                                                                   :top-left :fess
                                                                   :bottom-right :fess
                                                                   :top-left)}))
            :per-bend-sinister (options/pick default-options
                                             [[:line]
                                              [:origin]
                                              [:anchor]]
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
                                                [:anchor :point :choices] (case (-> field :origin :point (or :top-right))
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
                                                [:anchor :point :default] (case (-> field :origin :point (or :top-right))
                                                                            :top-right :fess
                                                                            :bottom-left :fess
                                                                            :top-right)}))
            :per-chevron (options/pick default-options
                                       [[:line]
                                        [:opposite-line]
                                        [:origin]
                                        [:direction-anchor]
                                        [:anchor]]
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
                                                                   (case (-> field :direction-anchor :point (or :bottom))
                                                                     :bottom [:bottom-left :bottom :bottom-right :left :right :angle]
                                                                     :top [:top-left :top :top-right :left :right :angle]
                                                                     :left [:top-left :left :bottom-left :top :bottom :angle]
                                                                     :right [:top-right :right :bottom-right :top :bottom :angle]
                                                                     :bottom-left [:bottom-left :bottom :bottom-right :top-left :left :angle]
                                                                     :bottom-right [:bottom-left :bottom :bottom-right :right :top-right :angle]
                                                                     :top-left [:top-left :top :top-right :left :bottom-left :angle]
                                                                     :top-right [:top-left :top :top-right :left :bottom-right :angle]
                                                                     [:top-left :top :top-right :left :right :bottom-left :bottom :bottom-right :angle]))
                                        [:anchor :point :default] (case (-> field :direction-anchor :point (or :bottom))
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
            :per-pile (options/pick default-options
                                    [[:origin]
                                     [:anchor]
                                     [:line]
                                     [:opposite-line]]
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
                                       [:geometry] {:size {:type :range
                                                           :min 5
                                                           :max 100
                                                           :default (case (-> field :geometry :size-mode (or :thickness))
                                                                      :thickness 75
                                                                      30)}
                                                    :size-mode {:type :choice
                                                                :choices [["Thickness" :thickness]
                                                                          ["Angle" :angle]]
                                                                :default :thickness}}
                                       [:origin :point :choices] (util/filter-choices
                                                                  position/anchor-point-choices
                                                                  [:top-left :top :top-right
                                                                   :left :right
                                                                   :bottom-left :bottom :bottom-right])
                                       [:origin :point :default] :top
                                       [:origin :alignment] (:alignment position/default-options)
                                       [:anchor :point :choices] (util/filter-choices
                                                                  position/anchor-point-choices
                                                                  (disj anchor-points (-> field :origin :point (or :top))))
                                       [:anchor :point :default] :fess
                                       [:anchor :alignment] nil
                                       [:anchor :angle :default] (cond
                                                                   (#{:top-left
                                                                      :top-right
                                                                      :bottom-left
                                                                      :bottom-right} (-> field :origin :point (or :top))) 45
                                                                   :else 0)
                                       [:anchor :angle :min] (cond
                                                               (#{:top-left
                                                                  :top-right
                                                                  :bottom-left
                                                                  :bottom-right} (-> field :origin :point (or :top))) 0
                                                               :else -90)
                                       [:anchor :angle :max] 90
                                       [:anchor :type] nil}))
            :per-saltire (options/pick default-options
                                       [[:line]
                                        [:opposite-line]
                                        [:origin]
                                        [:anchor]]
                                       {[:line] (-> line-style
                                                    (options/override-if-exists [:offset :min] 0)
                                                    (options/override-if-exists [:base-line] nil)
                                                    (dissoc :fimbriation))
                                        [:opposite-line] (-> opposite-line-style
                                                             (options/override-if-exists [:offset :min] 0)
                                                             (options/override-if-exists [:base-line] nil)
                                                             (dissoc :fimbriation))
                                        [:origin :alignment] nil
                                        [:anchor :point :choices] (util/filter-choices
                                                                   position/anchor-point-choices
                                                                   [:top-left :top-right :bottom-left :bottom-right :angle])})
            :quartered (options/pick default-options
                                     [[:line]
                                      [:opposite-line]
                                      [:origin :point]
                                      [:origin :offset-x]
                                      [:origin :offset-y]]
                                     {[:line] (-> line-style
                                                  (options/override-if-exists [:offset :min] 0)
                                                  (options/override-if-exists [:base-line] nil)
                                                  (dissoc :fimbriation))
                                      [:opposite-line] (-> opposite-line-style
                                                           (options/override-if-exists [:offset :min] 0)
                                                           (options/override-if-exists [:base-line] nil)
                                                           (dissoc :fimbriation))})
            :quarterly (options/pick default-options
                                     [[:layout :num-base-fields]
                                      [:layout :num-fields-x]
                                      [:layout :offset-x]
                                      [:layout :stretch-x]
                                      [:layout :num-fields-y]
                                      [:layout :offset-y]
                                      [:layout :stretch-y]]
                                     {[:layout :num-fields-x :default] 3
                                      [:layout :num-fields-y :default] 4})
            :gyronny (options/pick default-options
                                   [[:line]
                                    [:opposite-line]
                                    [:origin]
                                    [:anchor]]
                                   {[:line] (-> line-style
                                                (options/override-if-exists [:offset :min] 0)
                                                (options/override-if-exists [:base-line] nil)
                                                (dissoc :fimbriation))
                                    [:opposite-line] (-> opposite-line-style
                                                         (options/override-if-exists [:offset :min] 0)
                                                         (options/override-if-exists [:base-line] nil)
                                                         (dissoc :fimbriation))
                                    [:origin :alignment] nil
                                    [:anchor :point :choices] (util/filter-choices
                                                               position/anchor-point-choices
                                                               [:top-left :top-right :bottom-left :bottom-right :angle])})
            :paly (options/pick default-options
                                [[:line]
                                 [:layout :num-base-fields]
                                 [:layout :num-fields-x]
                                 [:layout :offset-x]
                                 [:layout :stretch-x]]
                                {[:line] line-style
                                 [:line :fimbriation] nil})
            :barry (options/pick default-options
                                 [[:line]
                                  [:layout :num-base-fields]
                                  [:layout :num-fields-y]
                                  [:layout :offset-y]
                                  [:layout :stretch-y]]
                                 {[:line] line-style
                                  [:line :fimbriation] nil})
            :chequy (options/pick default-options
                                  [[:layout :num-base-fields]
                                   [:layout :num-fields-x]
                                   [:layout :offset-x]
                                   [:layout :stretch-x]
                                   [:layout :num-fields-y]
                                   [:layout :offset-y]
                                   [:layout :stretch-y]]
                                  {[:layout :num-fields-y :default] nil})
            :lozengy (options/pick default-options
                                   [[:layout :num-fields-x]
                                    [:layout :offset-x]
                                    [:layout :stretch-x]
                                    [:layout :num-fields-y]
                                    [:layout :offset-y]
                                    [:layout :stretch-y]
                                    [:layout :rotation]]
                                   {[:layout :num-fields-y :default] nil
                                    [:layout :stretch-y :max] 3})
            :vairy (options/pick default-options
                                 [[:variant]
                                  [:layout :num-fields-x]
                                  [:layout :offset-x]
                                  [:layout :stretch-x]
                                  [:layout :num-fields-y]
                                  [:layout :offset-y]
                                  [:layout :stretch-y]]
                                 {[:layout :num-fields-y :default] nil})
            :potenty (options/pick default-options
                                   [[:variant]
                                    [:layout :num-fields-x]
                                    [:layout :offset-x]
                                    [:layout :stretch-x]
                                    [:layout :num-fields-y]
                                    [:layout :offset-y]
                                    [:layout :stretch-y]]
                                   {[:layout :num-fields-y :default] nil
                                    [:variant :choices] [["Default" :default]
                                                         ["Counter" :counter]
                                                         ["In pale" :in-pale]
                                                         ["En point" :en-point]]})
            :papellony (options/pick default-options
                                     [[:thickness]
                                      [:layout :num-fields-x]
                                      [:layout :offset-x]
                                      [:layout :stretch-x]
                                      [:layout :num-fields-y]
                                      [:layout :offset-y]
                                      [:layout :stretch-y]]
                                     {[:layout :num-fields-y :default] nil})
            :masonry (options/pick default-options
                                   [[:thickness]
                                    [:layout :num-fields-x]
                                    [:layout :offset-x]
                                    [:layout :stretch-x]
                                    [:layout :num-fields-y]
                                    [:layout :offset-y]
                                    [:layout :stretch-y]]
                                   {[:layout :num-fields-y :default] nil})
            :bendy (options/pick default-options
                                 [[:line]
                                  [:layout :num-base-fields]
                                  [:layout :num-fields-y]
                                  [:layout :offset-y]
                                  [:layout :stretch-y]
                                  [:origin]
                                  [:anchor]]
                                 (let [useful-points #{:top-left :bottom-right
                                                       :chief :honour :fess :nombril :base}
                                       point-choices (util/filter-choices
                                                      position/anchor-point-choices
                                                      useful-points)
                                       anchor-point-choices (util/filter-choices
                                                             position/anchor-point-choices
                                                             (conj useful-points :angle))]
                                   {[:line] (-> line-style
                                                (dissoc :fimbriation))
                                    [:origin :point :choices] point-choices
                                    [:origin :point :default] :top-left
                                    [:anchor :point :choices] (case (-> field :origin :point (or :top-left))
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
                                    [:anchor :point :default] (case (-> field :origin :point (or :top-left))
                                                                :top-left :fess
                                                                :bottom-right :fess
                                                                :top-left)}))
            :bendy-sinister (options/pick default-options
                                          [[:line]
                                           [:layout :num-base-fields]
                                           [:layout :num-fields-y]
                                           [:layout :offset-y]
                                           [:layout :stretch-y]
                                           [:origin]
                                           [:anchor]]
                                          (let [useful-points #{:top-right :bottom-left
                                                                :chief :honour :fess :nombril :base}
                                                point-choices (util/filter-choices
                                                               position/anchor-point-choices
                                                               useful-points)
                                                anchor-point-choices (util/filter-choices
                                                                      position/anchor-point-choices
                                                                      (conj useful-points :angle))]
                                            {[:line] (-> line-style
                                                         (dissoc :fimbriation))
                                             [:origin :point :choices] point-choices
                                             [:origin :point :default] :top-left
                                             [:anchor :point :choices] (case (-> field :origin :point (or :top-right))
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
                                             [:anchor :point :default] (case (-> field :origin :point (or :top-right))
                                                                         :top-right :fess
                                                                         :bottom-left :fess
                                                                         :top-right)}))
            :tierced-per-pale (options/pick default-options
                                            [[:line]
                                             [:layout :stretch-x]
                                             [:origin :point]
                                             [:origin :offset-x]]
                                            {[:origin :point :choices] position/point-choices-x
                                             [:line] line-style
                                             [:line :fimbriation] nil})
            :tierced-per-fess (options/pick default-options
                                            [[:line]
                                             [:layout :stretch-y]
                                             [:origin :point]
                                             [:origin :offset-y]]
                                            {[:origin :point :choices] position/point-choices-y
                                             [:line] line-style
                                             [:line :fimbriation] nil})
            :tierced-per-pairle (options/pick default-options
                                              [[:line]
                                               [:opposite-line]
                                               [:extra-line]
                                               [:origin]
                                               [:anchor]]
                                              {[:line] (-> line-style
                                                           (options/override-if-exists [:offset :min] 0)
                                                           (options/override-if-exists [:base-line] nil)
                                                           (dissoc :fimbriation))
                                               [:opposite-line] (-> opposite-line-style
                                                                    (options/override-if-exists [:offset :min] 0)
                                                                    (options/override-if-exists [:base-line] nil)
                                                                    (dissoc :fimbriation))
                                               [:extra-line] (-> extra-line-style
                                                                 (options/override-if-exists [:offset :min] 0)
                                                                 (options/override-if-exists [:base-line] nil)
                                                                 (dissoc :fimbriation))
                                               [:anchor :point :choices] (util/filter-choices
                                                                          position/anchor-point-choices
                                                                          [:top-left :top-right :angle])})
            :tierced-per-pairle-reversed (options/pick default-options
                                                       [[:line]
                                                        [:opposite-line]
                                                        [:extra-line]
                                                        [:origin]
                                                        [:anchor]]
                                                       {[:line] (-> line-style
                                                                    (options/override-if-exists [:offset :min] 0)
                                                                    (options/override-if-exists [:base-line] nil)
                                                                    (dissoc :fimbriation))
                                                        [:opposite-line] (-> opposite-line-style
                                                                             (options/override-if-exists [:offset :min] 0)
                                                                             (options/override-if-exists [:base-line] nil)
                                                                             (dissoc :fimbriation))
                                                        [:extra-line] (-> extra-line-style
                                                                          (options/override-if-exists [:offset :min] 0)
                                                                          (options/override-if-exists [:base-line] nil)
                                                                          (dissoc :fimbriation))
                                                        [:anchor :point :choices] (util/filter-choices
                                                                                   position/anchor-point-choices
                                                                                   [:bottom-left :bottom-right :angle])})

            {})
          (update-in [:anchor] (fn [anchor]
                                 (when anchor
                                   (position/adjust-options anchor (-> field :anchor)))))
          (update-in [:direction-anchor] (fn [direction-anchor]
                                           (when direction-anchor
                                             (position/adjust-options direction-anchor (-> field :direction-anchor)))))))))

(defn sanitize-opposite-line [field line]
  (-> (options/sanitize
       (util/deep-merge-with (fn [_current-value new-value]
                               new-value) line
                             (into {}
                                   (filter (fn [[_ v]]
                                             (some? v))
                                           (:opposite-line field))))
       (-> field options :opposite-line))
      (assoc :flipped? (if (-> field :opposite-line :flipped?)
                         (not (:flipped? line))
                         (:flipped? line)))
      (assoc :mirrored? (if (-> field :opposite-line :mirrored?)
                          (not (:mirrored? line))
                          (:mirrored? line)))))

(defn sanitize-extra-line [field line]
  (-> (options/sanitize
       (util/deep-merge-with (fn [_current-value new-value]
                               new-value) line
                             (into {}
                                   (filter (fn [[_ v]]
                                             (some? v))
                                           (:extra-line field))))
       (-> field options :extra-line))
      (assoc :flipped? (if (-> field :extra-line :flipped?)
                         (not (:flipped? line))
                         (:flipped? line)))
      (assoc :mirrored? (if (-> field :extra-line :mirrored?)
                          (not (:mirrored? line))
                          (:mirrored? line)))))
