(ns heraldry.coat-of-arms.division.options
  (:require [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.position :as position]
            [heraldry.coat-of-arms.shared.chevron :as chevron]
            [heraldry.util :as util]))

(defn diagonal-mode-choices [type]
  (let [options {:forty-five-degrees "45°"
                 :top-left-origin "Top-left to origin"
                 :top-right-origin "Top-right to origin"
                 :bottom-left-origin "Bottom-left to origin"
                 :bottom-right-origin "Bottom-right to origin"}]
    (->> type
         (get {:per-bend [:forty-five-degrees
                          :top-left-origin]
               :bendy [:forty-five-degrees
                       :top-left-origin]
               :per-bend-sinister [:forty-five-degrees
                                   :top-right-origin]
               :bendy-sinister [:forty-five-degrees
                                :top-right-origin]
               :per-chevron [:forty-five-degrees
                             :bottom-left-origin
                             :bottom-right-origin]
               :per-saltire [:forty-five-degrees
                             :top-left-origin
                             :top-right-origin
                             :bottom-left-origin
                             :bottom-right-origin]
               :gyronny [:forty-five-degrees
                         :top-left-origin
                         :top-right-origin
                         :bottom-left-origin
                         :bottom-right-origin]
               :tierced-per-pairle [:forty-five-degrees
                                    :top-left-origin
                                    :top-right-origin]
               :tierced-per-pairle-reversed [:forty-five-degrees
                                             :bottom-left-origin
                                             :bottom-right-origin]})
         (map (fn [key]
                [(get options key) key])))))

(def default-options
  {:line line/default-options
   :origin (-> position/default-options
               (dissoc :alignment))
   :anchor (-> position/anchor-default-options
               (dissoc :alignment))
   :diagonal-mode {:type :choice
                   :default :top-left-origin}
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

(defn options [division]
  (when division
    (let [line-style (line/options (:line division))]
      (-> (case (:type division)
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
                                       [:anchor :point :choices] (case (-> division :origin :point (or :top-left))
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
                                       [:anchor :point :default] (case (-> division :origin :point (or :top-left))
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
                                                [:anchor :point :choices] (case (-> division :origin :point (or :top-right))
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
                                                [:anchor :point :default] (case (-> division :origin :point (or :top-right))
                                                                            :top-right :fess
                                                                            :bottom-left :fess
                                                                            :top-right)}))
            :per-chevron (options/pick default-options
                                       [[:line]
                                        [:origin]
                                        [:anchor]
                                        [:variant]]
                                       {[:line] (-> line-style
                                                    (options/override-if-exists [:offset :min] 0))
                                        [:opposite-line] (-> line-style
                                                             (options/override-if-exists [:offset :min] 0))
                                        [:anchor :point :choices] (case (-> division :variant (or :base))
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
                                                                     [:bottom-left :bottom-right :angle]))
                                        [:variant :choices] chevron/variant-choices})
            :per-saltire (options/pick default-options
                                       [[:line]
                                        [:origin]
                                        [:anchor]
                                        [:anchor]]
                                       {[:line] (-> line-style
                                                    (options/override-if-exists [:offset :min] 0)
                                                    (dissoc :fimbriation))
                                        [:opposite-line] (-> line-style
                                                             (options/override-if-exists [:offset :min] 0)
                                                             (dissoc :fimbriation))
                                        [:origin :alignment] nil
                                        [:anchor :point :choices] (util/filter-choices
                                                                   position/anchor-point-choices
                                                                   [:top-left :top-right :bottom-left :bottom-right :angle])})
            :quartered (options/pick default-options
                                     [[:line]
                                      [:origin :point]
                                      [:origin :offset-x]
                                      [:origin :offset-y]]
                                     {[:line] line-style
                                      [:line :offset :min] 0
                                      [:line :fimbriation] nil})
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
                                    [:origin]
                                    [:anchor]]
                                   {[:line] (-> line-style
                                                (options/override-if-exists [:offset :min] 0)
                                                (dissoc :fimbriation))
                                    [:opposite-line] (-> line-style
                                                         (options/override-if-exists [:offset :min] 0)
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
                                    [:anchor :point :choices] (case (-> division :origin :point (or :top-left))
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
                                    [:anchor :point :default] (case (-> division :origin :point (or :top-left))
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
                                             [:anchor :point :choices] (case (-> division :origin :point (or :top-right))
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
                                             [:anchor :point :default] (case (-> division :origin :point (or :top-right))
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
                                               [:origin]
                                               [:anchor]]
                                              {[:line] (-> line-style
                                                           (options/override-if-exists [:offset :min] 0)
                                                           (dissoc :fimbriation))
                                               [:opposite-line] (-> line-style
                                                                    (options/override-if-exists [:offset :min] 0)
                                                                    (dissoc :fimbriation))
                                               [:anchor :point :choices] (util/filter-choices
                                                                          position/anchor-point-choices
                                                                          [:top-left :top-right :angle])})
            :tierced-per-pairle-reversed (options/pick default-options
                                                       [[:line]
                                                        [:origin]
                                                        [:anchor]]
                                                       {[:line] (-> line-style
                                                                    (options/override-if-exists [:offset :min] 0)
                                                                    (dissoc :fimbriation))
                                                        [:opposite-line] (-> line-style
                                                                             (options/override-if-exists [:offset :min] 0)
                                                                             (dissoc :fimbriation))
                                                        [:anchor :point :choices] (util/filter-choices
                                                                                   position/anchor-point-choices
                                                                                   [:bottom-left :bottom-right :angle])})

            {})
          (update-in [:anchor] (fn [anchor]
                                 (when anchor
                                   (position/adjust-options anchor (-> division :anchor)))))))))
