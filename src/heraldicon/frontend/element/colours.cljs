(ns heraldicon.frontend.element.colours
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.element.checkbox :as checkbox]
   [heraldicon.frontend.element.core :as element]
   [heraldicon.frontend.js-event :as js-event]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.library.charge.details :as charge.details]
   [heraldicon.frontend.macros :as macros]
   [heraldicon.heraldry.option.attributes :as attributes]
   [heraldicon.interface :as interface]
   [heraldicon.util.colour :as colour]
   [heraldicon.util.core :as util]
   [re-frame.core :as rf]))

(defn- choice-keywords [choices]
  (->> choices
       (tree-seq vector? seq)
       (keep (fn [value]
               (when (and (vector? value)
                          (= (count value) 2)
                          (-> value second keyword?))
                 (second value))))
       vec))

(macros/reg-event-db ::set-colour-value
  (fn [db [_ context colour value selected-colours]]
    (let [normalized-colour (colour/normalize colour)]
      (if (get selected-colours normalized-colour)
        (loop [db db
               [colour & rest] (keys (get-in db (:path context)))]
          (if colour
            (let [new-db (cond-> db
                           (get selected-colours (colour/normalize colour))
                           (assoc-in (:path (c/++ context colour)) value))]
              (recur new-db rest))
            db))
        (assoc-in db (:path (c/++ context colour)) value)))))

(defn- set-colour-qualifier [current qualifier]
  (let [[value _] (if (vector? current)
                    current
                    [current nil])]
    (if (= qualifier :none)
      value
      [value qualifier])))

(macros/reg-event-db ::set-colour-qualifier
  (fn [db [_ context colour qualifier selected-colours]]
    (let [normalized-colour (colour/normalize colour)]
      (if (get selected-colours normalized-colour)
        (loop [db db
               [colour & rest] (keys (get-in db (:path context)))]
          (if colour
            (let [new-db (cond-> db
                           (get selected-colours (colour/normalize colour))
                           (update-in (:path (c/++ context colour)) set-colour-qualifier qualifier))]
              (recur new-db rest))
            db))
        (update-in db (:path (c/++ context colour)) set-colour-qualifier qualifier)))))

(defmethod element/element :ui.element/colours [{:keys [path] :as context}]
  (let [colours (interface/get-raw-data context)
        sort-column (or (interface/get-raw-data (c/<< context :path [:ui :colours :sort path]))
                        :colour)
        sort-fn (case sort-column
                  :modifier (fn [[colour value]]
                              [(-> value
                                   attributes/tincture-modifier
                                   (util/index-of
                                    (choice-keywords attributes/tincture-modifier-for-charge-choices)))
                               (-> value
                                   attributes/tincture-modifier-qualifier
                                   (util/index-of
                                    (choice-keywords attributes/tincture-modifier-qualifier-choices)))
                               colour])
                  :qualifier (fn [[colour value]]
                               [(-> value
                                    attributes/tincture-modifier-qualifier
                                    (util/index-of
                                     (choice-keywords attributes/tincture-modifier-qualifier-choices)))
                                (-> value
                                    attributes/tincture-modifier
                                    (util/index-of
                                     (choice-keywords attributes/tincture-modifier-for-charge-choices)))
                                colour])
                  ;; default to the colour itself
                  first)
        header-td-style {:border-bottom "1px solid #888"
                         :padding-left "0.5em"}
        selected-colours @(rf/subscribe [::charge.details/selected-colours])]
    [:<>
     [:div.ui-setting {:style {:margin-top "10px"
                               :white-space "nowrap"
                               :max-height "30em"
                               :overflow "scroll"}}
      [:label [tr :string.render-options.mode-choice/colours]]
      [:div.option
       (if (seq colours)
         [:table.charge-colours {:cell-spacing 0}
          [:thead
           [:tr
            [:td {:style (dissoc header-td-style :padding-left)}
             [:a {:href "#"
                  :on-click (js-event/handled #(rf/dispatch [:set [:ui :colours :sort path] :colour]))} "#"
              (when (= sort-column :colour)
                [:i.fas.fa-sort {:style {:margin-left "5px"}}])]]
            [:td {:style header-td-style}
             [:a {:href "#"
                  :on-click (js-event/handled #(rf/dispatch [:set [:ui :colours :sort path] :modifier]))}
              [tr :string.option/function]
              (when (= sort-column :modifier)
                [:i.fas.fa-sort {:style {:margin-left "5px"}}])]]
            [:td {:style header-td-style}
             [:a {:href "#"
                  :on-click (js-event/handled #(rf/dispatch [:set [:ui :colours :sort path] :qualifier]))}
              [tr :string.option/shading]
              (when (= sort-column :qualifier)
                [:i.fas.fa-sort {:style {:margin-left "5px"}}])]]
            [:td {:style header-td-style}
             [tr :string.option/highlight]]]
           [:tr {:style {:height "0.5em"}}
            [:td]
            [:td]
            [:td]
            [:td {:style {:padding-left "1em"
                          :border-left "1px solid #888"}}]]]
          (into [:tbody]
                (map (fn [[colour value]]
                       (let [[value qualifier] (if (vector? value)
                                                 value
                                                 [value :none])
                             selected? (get selected-colours (colour/normalize colour))]
                         ^{:key colour}
                         [:tr {:class (when selected?
                                        "selected")}
                          [:td {:style {:width "1.6em"}}
                           [:div.colour-preview.tooltip {:style {:background-color colour}}
                            [:div.bottom {:style {:top "30px"}}
                             [:h3 {:style {:text-align "center"}} colour]]]]

                          [:td {:style {:padding-left "0.5em"}}
                           (into [:select {:value (util/keyword->str value)
                                           :on-change #(rf/dispatch
                                                        [::set-colour-value
                                                         context
                                                         colour
                                                         (keyword (-> % .-target .-value))
                                                         selected-colours])
                                           :style {:vertical-align "top"}}]
                                 (map (fn [[group-name & group-choices]]
                                        (if (and (-> group-choices count (= 1))
                                                 (-> group-choices first keyword?))
                                          (let [key (first group-choices)]
                                            ^{:key key}
                                            [:option {:value (util/keyword->str key)}
                                             (tr group-name)])

                                          (into
                                           ^{:key group-name}
                                           [:optgroup {:label (tr group-name)}]
                                           (map (fn [[display-name key]]
                                                  ^{:key key}
                                                  [:option {:value (util/keyword->str key)}
                                                   (tr display-name)]))
                                           group-choices))))
                                 attributes/tincture-modifier-for-charge-choices)]

                          [:td {:style {:padding-left "0.5em"
                                        :padding-right "0.5em"}}
                           (when-not (#{:keep
                                        :outline
                                        :shadow
                                        :highlight
                                        :layer-separator} value)
                             (into [:select {:value qualifier
                                             :on-change #(rf/dispatch
                                                          [::set-colour-qualifier
                                                           context
                                                           colour
                                                           (keyword (-> % .-target .-value))
                                                           selected-colours])
                                             :style {:vertical-align "top"}}]
                                   (map (fn [[group-name & group-choices]]
                                          (if (and (-> group-choices count (= 1))
                                                   (-> group-choices first keyword?))
                                            (let [key (first group-choices)]
                                              ^{:key key}
                                              [:option {:value (util/keyword->str key)}
                                               (tr group-name)])
                                            (into
                                             ^{:key group-name}
                                             [:optgroup {:label (tr group-name)}]
                                             (map (fn [[display-name key]]
                                                    ^{:key key}
                                                    [:option {:value (util/keyword->str key)}
                                                     (tr display-name)]))
                                             group-choices))))
                                   attributes/tincture-modifier-qualifier-choices))]
                          [:td {:style {:padding-left "1em"
                                        :border-left "1px solid #888"}}
                           [checkbox/checkbox (c/<< context :path (conj charge.details/show-colours-path colour))
                            :option {:type :option.type/boolean}]]])))
                (sort-by sort-fn colours))]
         [tr :string.miscellaneous/none])]]]))
