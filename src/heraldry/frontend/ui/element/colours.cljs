(ns heraldry.frontend.ui.element.colours
  (:require
   [heraldry.coat-of-arms.attributes :as attributes]
   [heraldry.frontend.language :refer [tr]]
   [heraldry.frontend.state :as state]
   [heraldry.frontend.ui.element.checkbox :as checkbox]
   [heraldry.frontend.ui.interface :as ui-interface]
   [heraldry.strings :as strings]
   [heraldry.util :as util]
   [re-frame.core :as rf]))

(defn choice-keywords [choices]
  (->> choices
       (tree-seq vector? seq)
       (keep (fn [value]
               (when (and (vector? value)
                          (or (-> value first string?)
                              (-> value first map?))
                          (-> value second keyword?))
                 (second value))))
       vec))

(defn form [path]
  (let [colours @(rf/subscribe [:get path])
        sort-column (or @(rf/subscribe [:get [:ui :colours :sort path]])
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
                         :padding-left "0.5em"}]
    [:<>
     [:div.ui-setting {:style {:margin-top "10px"
                               :white-space "nowrap"
                               :max-height "30em"
                               :overflow "scroll"}}
      [:label [tr {:en "Colours"
                   :de "Farben"}]]
      [:div.option
       (if (seq colours)
         [:table {:cell-spacing 0}
          [:thead
           [:tr
            [:td {:style (dissoc header-td-style :padding-left)}
             [:a {:href "#"
                  :on-click #(state/dispatch-on-event
                              % [:set [:ui :colours :sort path] :colour])} "#"
              (when (= sort-column :colour)
                [:i.fas.fa-sort {:style {:margin-left "5px"}}])]]
            [:td {:style header-td-style}
             [:a {:href "#"
                  :on-click #(state/dispatch-on-event
                              % [:set [:ui :colours :sort path] :modifier])}
              [tr {:en "Function"
                   :de "Funktion"}]
              (when (= sort-column :modifier)
                [:i.fas.fa-sort {:style {:margin-left "5px"}}])]]
            [:td {:style header-td-style}
             [:a {:href "#"
                  :on-click #(state/dispatch-on-event
                              % [:set [:ui :colours :sort path] :qualifier])}
              [tr {:en "Shading"
                   :de "Schattierung"}]
              (when (= sort-column :qualifier)
                [:i.fas.fa-sort {:style {:margin-left "5px"}}])]]
            [:td {:style header-td-style}
             [tr {:en "Highlight"
                  :de "Highlight"}]]]
           [:tr {:style {:height "0.5em"}}
            [:td]
            [:td]
            [:td]
            [:td {:style {:padding-left "1em"
                          :border-left "1px solid #888"}}]]]
          [:tbody
           (doall
            (for [[colour value] (sort-by sort-fn colours)]
              (let [[value qualifier] (if (vector? value)
                                        value
                                        [value :none])]
                ^{:key colour}
                [:tr
                 [:td {:style {:width "1.6em"}}
                  [:div.colour-preview.tooltip {:style {:background-color colour}}
                   [:div.bottom {:style {:top "30px"}}
                    [:h3 {:style {:text-align "center"}} colour]]]]

                 [:td {:style {:padding-left "0.5em"}}
                  [:select {:value (util/keyword->str value)
                            :on-change #(let [selected (keyword (-> % .-target .-value))]
                                          (rf/dispatch [:set (conj path colour) selected]))
                            :style {:vertical-align "top"}}
                   (doall
                    (for [[group-name & group-choices] attributes/tincture-modifier-for-charge-choices]
                      (if (and (-> group-choices count (= 1))
                               (-> group-choices first keyword?))
                        (let [key (-> group-choices first)]
                          ^{:key key}
                          [:option {:value (util/keyword->str key)}
                           (tr group-name)])
                        ^{:key group-name}
                        [:optgroup {:label (tr group-name)}
                         (doall
                          (for [[display-name key] group-choices]
                            ^{:key key}
                            [:option {:value (util/keyword->str key)}
                             (tr display-name)]))])))]]

                 [:td {:style {:padding-left "0.5em"
                               :padding-right "0.5em"}}
                  (when-not (#{:keep
                               :outline
                               :shadow
                               :highlight
                               :layer-separator} value)
                    [:select {:value qualifier
                              :on-change #(let [selected (keyword (-> % .-target .-value))]
                                            (rf/dispatch [:set (conj path colour) (if (= selected :none)
                                                                                    value
                                                                                    [value selected])]))
                              :style {:vertical-align "top"}}
                     (doall
                      (for [[group-name & group-choices] attributes/tincture-modifier-qualifier-choices]
                        (if (and (-> group-choices count (= 1))
                                 (-> group-choices first keyword?))
                          (let [key (-> group-choices first)]
                            ^{:key key}
                            [:option {:value (util/keyword->str key)}
                             (tr group-name)])
                          ^{:key group-name}
                          [:optgroup {:label (tr group-name)}
                           (doall
                            (for [[display-name key] group-choices]
                              ^{:key key}
                              [:option {:value (util/keyword->str key)}
                               (tr display-name)]))])))])]
                 [:td {:style {:padding-left "1em"
                               :border-left "1px solid #888"}}
                  [checkbox/checkbox [:ui :colours :show colour]
                   :option {:type :boolean}]]])))]]
         [tr strings/none])]]]))

(defmethod ui-interface/form-element :colours [{:keys [path]}]
  [form path])
