(ns heraldry.coat-of-arms.division.options
  (:require [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.position :as position]))

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
   :origin position/default-options
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
      (case (:type division)
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
                                 [:origin :point]
                                 [:origin :offset-y]
                                 [:diagonal-mode]]
                                {[:diagonal-mode :choices] (diagonal-mode-choices :per-bend)
                                 [:origin :point :choices] position/point-choices-y
                                 [:line] line-style})
        :per-bend-sinister (options/pick default-options
                                         [[:line]
                                          [:origin :point]
                                          [:origin :offset-y]
                                          [:diagonal-mode]]
                                         {[:diagonal-mode :choices] (diagonal-mode-choices :per-bend-sinister)
                                          [:diagonal-mode :default] :top-right-origin
                                          [:origin :point :choices] position/point-choices-y
                                          [:line] line-style})
        :per-chevron (options/pick default-options
                                   [[:line]
                                    [:origin :point]
                                    [:origin :offset-x]
                                    [:origin :offset-y]
                                    [:diagonal-mode]]
                                   {[:diagonal-mode :choices] (diagonal-mode-choices :per-chevron)
                                    [:diagonal-mode :default] :forty-five-degrees
                                    [:origin :point :choices] position/point-choices-y
                                    [:line] line-style
                                    [:line :offset :min] 0})
        :per-saltire (options/pick default-options
                                   [[:line]
                                    [:origin :point]
                                    [:origin :offset-x]
                                    [:origin :offset-y]
                                    [:diagonal-mode]]
                                   {[:diagonal-mode :choices] (diagonal-mode-choices :per-saltire)
                                    [:line] line-style
                                    [:line :offset :min] 0
                                    [:line :fimbriation] nil})
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
                                [:origin :point]
                                [:origin :offset-x]
                                [:origin :offset-y]
                                [:diagonal-mode]]
                               {[:diagonal-mode :choices] (diagonal-mode-choices :gyronny)
                                [:line] line-style
                                [:line :offset :min] 0
                                [:line :fimbriation] nil})
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
                              [:origin :point]
                              [:origin :offset-x]
                              [:origin :offset-y]
                              [:diagonal-mode]]
                             {[:diagonal-mode :choices] (diagonal-mode-choices :bendy)
                              [:origin :point :choices] position/point-choices-y
                              [:line] line-style
                              [:line :fimbriation] nil})
        :bendy-sinister (options/pick default-options
                                      [[:line]
                                       [:layout :num-base-fields]
                                       [:layout :num-fields-y]
                                       [:layout :offset-y]
                                       [:layout :stretch-y]
                                       [:origin :point]
                                       [:origin :offset-x]
                                       [:origin :offset-y]
                                       [:diagonal-mode]]
                                      {[:diagonal-mode :choices] (diagonal-mode-choices :bendy)
                                       [:diagonal-mode :default] :top-right-origin
                                       [:origin :point :choices] position/point-choices-y
                                       [:line] line-style
                                       [:line :fimbriation] nil})
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
                                           [:origin :point]
                                           [:origin :offset-x]
                                           [:origin :offset-y]
                                           [:diagonal-mode]]
                                          {[:diagonal-mode :choices] (diagonal-mode-choices :tierced-per-pairle)
                                           [:line] line-style
                                           [:line :offset :min] 0
                                           [:line :fimbriation] nil})
        :tierced-per-pairle-reversed (options/pick default-options
                                                   [[:line]
                                                    [:origin :point]
                                                    [:origin :offset-x]
                                                    [:origin :offset-y]
                                                    [:diagonal-mode]]
                                                   {[:diagonal-mode :choices] (diagonal-mode-choices :tierced-per-pairle-reversed)
                                                    [:diagonal-mode :default] :forty-five-debrees
                                                    [:line] line-style
                                                    [:line :offset :min] 0
                                                    [:line :fimbriation] nil})
        {}))))
