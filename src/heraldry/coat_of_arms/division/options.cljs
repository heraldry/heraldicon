(ns heraldry.coat-of-arms.division.options
  (:require [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.position :as position]))

(defn diagonal-mode-choices [type]
  (let [options {:forty-five-degrees  "45°"
                 :top-left-origin     "Top-left to origin"
                 :top-right-origin    "Top-right to origin"
                 :bottom-left-origin  "Bottom-left to origin"
                 :bottom-right-origin "Bottom-right to origin"}]
    (->> type
         (get {:per-bend                    [:forty-five-degrees
                                             :top-left-origin]
               :bendy                       [:forty-five-degrees
                                             :top-left-origin]
               :per-bend-sinister           [:forty-five-degrees
                                             :top-right-origin]
               :bendy-sinister              [:forty-five-degrees
                                             :top-right-origin]
               :per-chevron                 [:forty-five-degrees
                                             :bottom-left-origin
                                             :bottom-right-origin]
               :per-saltire                 [:forty-five-degrees
                                             :top-left-origin
                                             :top-right-origin
                                             :bottom-left-origin
                                             :bottom-right-origin]
               :gyronny                     [:forty-five-degrees
                                             :top-left-origin
                                             :top-right-origin
                                             :bottom-left-origin
                                             :bottom-right-origin]
               :tierced-per-pairle          [:forty-five-degrees
                                             :top-left-origin
                                             :top-right-origin]
               :tierced-per-pairle-reversed [:forty-five-degrees
                                             :bottom-left-origin
                                             :bottom-right-origin]})
         (map (fn [key]
                [(get options key) key])))))

(def default-options
  {:line          line/default-options
   :origin        position/default-options
   :diagonal-mode {:type    :choice
                   :default :top-left-origin}
   :layout        {:num-fields-x    {:type     :range
                                     :min      2
                                     :max      20
                                     :default  6
                                     :integer? true}
                   :num-fields-y    {:type     :range
                                     :min      2
                                     :max      20
                                     :default  6
                                     :integer? true}
                   :num-base-fields {:type     :range
                                     :min      2
                                     :max      8
                                     :default  2
                                     :integer? true}
                   :offset-x        {:type    :range
                                     :min     -1
                                     :max     1
                                     :default 0}
                   :offset-y        {:type    :range
                                     :min     -1
                                     :max     1
                                     :default 0}
                   :stretch-x       {:type    :range
                                     :min     0.5
                                     :max     2
                                     :default 1}
                   :stretch-y       {:type    :range
                                     :min     0.5
                                     :max     2
                                     :default 1}
                   :rotation        {:type    :range
                                     :min     -90
                                     :max     90
                                     :default 0}}})

(defn pick-options [paths & values]
  (let [values  (first values)
        options (loop [options       {}
                       [path & rest] paths]
                  (let [next-options (-> options
                                         (assoc-in path (get-in default-options path)))]
                    (if (nil? rest)
                      next-options
                      (recur next-options rest))))]
    (loop [options              options
           [[key value] & rest] values]
      (let [next-options (if key
                           (assoc-in options key value)
                           options)]
        (if (nil? rest)
          next-options
          (recur next-options rest))))))

(defn options [division]
  (when division
    (->
     (case (:type division)
       :per-pale                    (pick-options [[:line]
                                                   [:origin :point]
                                                   [:origin :offset-x]]
                                                  {[:origin :point :choices] position/point-choices-x})
       :per-fess                    (pick-options [[:line]
                                                   [:origin :point]
                                                   [:origin :offset-y]]
                                                  {[:origin :point :choices] position/point-choices-y})
       :per-bend                    (pick-options [[:line]
                                                   [:origin :point]
                                                   [:origin :offset-y]
                                                   [:diagonal-mode]]
                                                  {[:diagonal-mode :choices] (diagonal-mode-choices :per-bend)
                                                   [:origin :point :choices] position/point-choices-y})
       :per-bend-sinister           (pick-options [[:line]
                                                   [:origin :point]
                                                   [:origin :offset-y]
                                                   [:diagonal-mode]]
                                                  {[:diagonal-mode :choices] (diagonal-mode-choices :per-bend-sinister)
                                                   [:diagonal-mode :default] :top-right-origin
                                                   [:origin :point :choices] position/point-choices-y})
       :per-chevron                 (pick-options [[:line]
                                                   [:origin :point]
                                                   [:origin :offset-x]
                                                   [:origin :offset-y]
                                                   [:diagonal-mode]]
                                                  {[:diagonal-mode :choices] (diagonal-mode-choices :per-chevron)
                                                   [:diagonal-mode :default] :forty-five-degrees
                                                   [:origin :point :choices] position/point-choices-y
                                                   [:line :offset :min]      0})
       :per-saltire                 (pick-options [[:line]
                                                   [:origin :point]
                                                   [:origin :offset-x]
                                                   [:origin :offset-y]
                                                   [:diagonal-mode]]
                                                  {[:diagonal-mode :choices] (diagonal-mode-choices :per-saltire)
                                                   [:line :offset :min]      0
                                                   [:line :fimbriation]      nil})
       :quartered                   (pick-options [[:line]
                                                   [:origin :point]
                                                   [:origin :offset-x]
                                                   [:origin :offset-y]]
                                                  {[:line :offset :min] 0
                                                   [:line :fimbriation] nil})
       :quarterly                   (pick-options [[:layout :num-base-fields]
                                                   [:layout :num-fields-x]
                                                   [:layout :offset-x]
                                                   [:layout :stretch-x]
                                                   [:layout :num-fields-y]
                                                   [:layout :offset-y]
                                                   [:layout :stretch-y]]
                                                  {[:layout :num-fields-x :default] 3
                                                   [:layout :num-fields-y :default] 4
                                                   [:line :fimbriation]             nil})
       :gyronny                     (pick-options [[:line]
                                                   [:origin :point]
                                                   [:origin :offset-x]
                                                   [:origin :offset-y]
                                                   [:diagonal-mode]]
                                                  {[:diagonal-mode :choices] (diagonal-mode-choices :gyronny)
                                                   [:line :offset :min]      0
                                                   [:line :fimbriation]      nil})
       :paly                        (pick-options [[:line]
                                                   [:layout :num-base-fields]
                                                   [:layout :num-fields-x]
                                                   [:layout :offset-x]
                                                   [:layout :stretch-x]]
                                                  {[:line :fimbriation] nil})
       :barry                       (pick-options [[:line]
                                                   [:layout :num-base-fields]
                                                   [:layout :num-fields-y]
                                                   [:layout :offset-y]
                                                   [:layout :stretch-y]]
                                                  {[:line :fimbriation] nil})
       :chequy                      (pick-options [[:layout :num-base-fields]
                                                   [:layout :num-fields-x]
                                                   [:layout :offset-x]
                                                   [:layout :stretch-x]
                                                   [:layout :num-fields-y]
                                                   [:layout :offset-y]
                                                   [:layout :stretch-y]]
                                                  {[:layout :num-fields-y :default] nil
                                                   [:line :fimbriation]             nil})
       :lozengy                     (pick-options [[:layout :num-fields-x]
                                                   [:layout :offset-x]
                                                   [:layout :stretch-x]
                                                   [:layout :num-fields-y]
                                                   [:layout :offset-y]
                                                   [:layout :stretch-y]
                                                   [:layout :rotation]]
                                                  {[:layout :num-fields-y :default] nil
                                                   [:layout :stretch-y :max]        3
                                                   [:line :fimbriation]             nil})
       :bendy                       (pick-options [[:line]
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
                                                   [:line :fimbriation]      nil})
       :bendy-sinister              (pick-options [[:line]
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
                                                   [:line :fimbriation]      nil})
       :tierced-per-pale            (pick-options [[:line]
                                                   [:layout :stretch-x]
                                                   [:origin :point]
                                                   [:origin :offset-x]]
                                                  {[:origin :point :choices] position/point-choices-x
                                                   [:line :fimbriation]      nil})
       :tierced-per-fess            (pick-options [[:line]
                                                   [:layout :stretch-y]
                                                   [:origin :point]
                                                   [:origin :offset-y]]
                                                  {[:origin :point :choices] position/point-choices-y
                                                   [:line :fimbriation]      nil})
       :tierced-per-pairle          (pick-options [[:line]
                                                   [:origin :point]
                                                   [:origin :offset-x]
                                                   [:origin :offset-y]
                                                   [:diagonal-mode]]
                                                  {[:diagonal-mode :choices] (diagonal-mode-choices :tierced-per-pairle)
                                                   [:line :offset :min]      0
                                                   [:line :fimbriation]      nil})
       :tierced-per-pairle-reversed (pick-options [[:line]
                                                   [:origin :point]
                                                   [:origin :offset-x]
                                                   [:origin :offset-y]
                                                   [:diagonal-mode]]
                                                  {[:diagonal-mode :choices] (diagonal-mode-choices :tierced-per-pairle-reversed)
                                                   [:diagonal-mode :default] :forty-five-debrees
                                                   [:line :offset :min]      0
                                                   [:line :fimbriation]      nil})
       {})
     (update-in [:line] (fn [line]
                          (when line
                            (options/merge (line/options (get-in division [:line]))
                                           line)))))))
