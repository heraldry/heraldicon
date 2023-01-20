(ns heraldicon.frontend.element.colours
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.element.checkbox :as checkbox]
   [heraldicon.frontend.element.core :as element]
   [heraldicon.frontend.js-event :as js-event]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.library.charge.details :as charge.details]
   [heraldicon.frontend.macros :as macros]
   [heraldicon.frontend.tooltip :as tooltip]
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

(defn- supports-qualifier? [value]
  (not (#{:keep
          :outline
          :shadow
          :highlight
          :layer-separator} value)))

(macros/reg-event-fx ::set-colour-value
  (fn [{:keys [db]} [_ context colour value selected-colours]]
    (let [colours-to-change (if (get selected-colours colour)
                              selected-colours
                              #{colour})
          [new-db
           reprocess-svg?] (loop [current-db db
                                  reprocess-svg? false
                                  [colour & rest] (keys (get-in db (:path context)))]
                             (if colour
                               (if (get colours-to-change colour)
                                 (let [old-value (get-in current-db (:path (c/++ context colour)))
                                       new-db (assoc-in current-db (:path (c/++ context colour)) value)
                                       new-reprocess-svg? (or reprocess-svg?
                                                              (= old-value :layer-separator)
                                                              (= value :layer-separator))]
                                   (recur new-db new-reprocess-svg? rest))
                                 (recur current-db reprocess-svg? rest))
                               [current-db reprocess-svg?]))]
      (cond-> {:db new-db}
        reprocess-svg? (assoc :fx [[:dispatch [::charge.details/reprocess-svg-file
                                               (pop (:path context))]]])))))

(defn- set-colour-qualifier [current qualifier]
  (let [[value _] (if (vector? current)
                    current
                    [current nil])]
    (if (= qualifier :none)
      value
      [value qualifier])))

(defn- set-shading-reference [db context reference-colour]
  (loop [db db
         [colour & rest] (keys (get-in db (:path context)))]
    (if colour
      (recur (update-in db
                        (:path (c/++ context colour))
                        (fn [current]
                          (let [[value qualifier] (attributes/parse-colour-value-and-qualifier current)
                                qualifier (cond
                                            (= colour reference-colour) :reference
                                            (= qualifier :reference) nil
                                            :else qualifier)]
                            (if qualifier
                              [value qualifier]
                              value))))
             rest)
      db)))

(macros/reg-event-db ::set-colour-qualifier
  (fn [db [_ context colour qualifier]]
    (if (= qualifier :reference)
      (set-shading-reference db context colour)
      (update-in db (:path (c/++ context colour)) set-colour-qualifier qualifier))))

(defn- closest-shadow-qualifier [value]
  (let [multiple-of-5 (-> (int (* (Math/ceil (/ (Math/abs value) 5)) 5))
                          (max 0)
                          (min 95))]
    (attributes/make-qualifier-keyword :shadow multiple-of-5)))

(defn- closest-highlight-qualifier [value]
  (let [multiple-of-5 (-> (int (* (Math/ceil (/ (Math/abs value) 5)) 5))
                          (max 0)
                          (min 95))]
    (attributes/make-qualifier-keyword :highlight multiple-of-5)))

(defn- determine-qualifier [colour reference-brightness]
  (let [brightness (colour/brightness colour)
        reference-brightness (max reference-brightness 10)
        brightness (max brightness 10)
        difference (* (- (/ brightness reference-brightness) 1) 100)]
    (cond
      (neg? difference) (closest-shadow-qualifier difference)
      (pos? difference) (closest-highlight-qualifier difference)
      :else nil)))

(defn- detect-shading [db context reference-colour selected-colours]
  (let [reference-brightness (colour/brightness reference-colour)]
    (loop [db db
           [colour & rest] (keys (get-in db (:path context)))]
      (if colour
        (let [new-db (cond-> db
                       (get selected-colours colour)
                       (update-in (:path (c/++ context colour))
                                  (fn [current]
                                    (let [[value qualifier] (attributes/parse-colour-value-and-qualifier current)]
                                      (if (= qualifier :reference)
                                        current
                                        (let [new-qualifier (when (supports-qualifier? value)
                                                              (determine-qualifier colour reference-brightness))]
                                          (if new-qualifier
                                            [value new-qualifier]
                                            value)))))))]
          (recur new-db rest))
        db))))

(macros/reg-event-db ::detect-shading
  (fn [db [_ context selected-colours]]
    (let [reference-colour (first (keep (fn [[k v]]
                                          (let [[_ qualifier] (attributes/parse-colour-value-and-qualifier v)]
                                            (when (= qualifier :reference)
                                              k)))
                                        (get-in db (:path context))))]
      (cond
        (empty? selected-colours) (do
                                    (js/alert (tr :string.text.charge-library/no-colours-selected))
                                    db)
        (not reference-colour) (do
                                 (js/alert (tr :string.text.charge-library/no-shading-reference))
                                 db)
        :else (detect-shading db context reference-colour selected-colours)))))

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
                               :white-space "nowrap"}}
      [:label [tr :string.render-options.mode-choice/colours]]
      [:div {:style {:display "inlinee-block"
                     :float "right"}}
       [:span {:style {:margin-right "0.25em"}}
        [tooltip/info :string.tooltip/detect-shading]]
       [:button {:on-click #(rf/dispatch [::detect-shading context selected-colours])}
        [tr :string.button/detect-shading]]]
      [:br]
      [:div.option {:style {:transform "translate(-50%, 0)"
                            :left "50%"
                            :max-height "30em"
                            :overflow "scroll"}}
       (if (seq colours)
         [:table.charge-colours {:cell-spacing 0}
          [:thead
           [:tr
            [:td {:style header-td-style}]
            [:td {:style header-td-style}
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
                [:i.fas.fa-sort {:style {:margin-left "5px"}}])]]]
           [:tr {:style {:height "0.5em"}}
            [:td]
            [:td]
            [:td]
            [:td]]]
          (into [:tbody]
                (map (fn [[colour value]]
                       (let [[value qualifier] (attributes/parse-colour-value-and-qualifier value)
                             qualifier (or qualifier :none)
                             selected? (get selected-colours colour)]
                         ^{:key colour}
                         [:tr {:class (when selected?
                                        "selected")}
                          [:td {:style {:padding-left "0.5em"}}
                           [checkbox/checkbox (c/<< context :path (conj charge.details/show-colours-path colour))
                            :option {:type :option.type/boolean}]]
                          [:td {:style {:width "2.1em"
                                        :padding-left "0.7em"}}
                           [:div.colour-preview.tooltip
                            {:on-click (js-event/handled
                                        #(rf/dispatch [::charge.details/toggle-select-colour colour]))
                             :style {:background-color colour
                                     :cursor "pointer"}}
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
                           (when (supports-qualifier? value)
                             (into [:select {:value qualifier
                                             :on-change #(rf/dispatch
                                                          [::set-colour-qualifier
                                                           context
                                                           colour
                                                           (keyword (-> % .-target .-value))])
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
                                   attributes/tincture-modifier-qualifier-choices))]])))
                (sort-by sort-fn colours))]
         [tr :string.miscellaneous/none])]]]))
