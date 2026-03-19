(ns heraldicon.frontend.element.theme-select
  (:require
   [clojure.string :as str]
   [heraldicon.frontend.element.core :as element]
   [heraldicon.frontend.element.submenu :as submenu]
   [heraldicon.frontend.element.value-mode-select :as value-mode-select]
   [heraldicon.frontend.js-event :as js-event]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.tooltip :as tooltip]
   [heraldicon.interface :as interface]
   [heraldicon.options :as options]
   [heraldicon.static :as static]
   [re-frame.core :as rf]
   [reagent.core :as r]))

(defn- matches-search? [display-name search-str]
  (let [lower-name (str/lower-case (tr display-name))
        terms (str/split (str/lower-case (str/trim search-str)) #"\s+")]
    (every? #(str/includes? lower-name %) terms)))

(defn- theme-choice [context key display-name & {:keys [selected?
                                                        clickable?]
                                                 :or {clickable? true}}]
  (let [choice [:div {:style {:display "flex"
                              :flex-direction "column"
                              :align-items "center"
                              :width "4em"}}
                [:div {:style {:border (if selected?
                                         "1px solid #000"
                                         "1px solid transparent")
                               :border-radius "5px"}}
                 [:img.clickable {:style {:width "4em"
                                          :height (when-not (= key :all) "4.5em")}
                                  :on-click (when clickable?
                                              (js-event/handled #(rf/dispatch [:set context key])))
                                  :src (static/static-url (if (= key :all)
                                                            "/img/psychedelic.png"
                                                            (str "/svg/theme-" (name key) ".svg")))}]]
                (when clickable?
                  [:div.choice-label (tr display-name)])]]
    (if clickable?
      [tooltip/choice display-name choice]
      choice)))

(defmethod element/element :ui.element/theme-select [context]
  (let [search (r/atom "")]
    (fn []
      (when-not @(rf/subscribe [::submenu/open? (:path context)])
        (reset! search ""))
      (when-let [option (interface/get-options context)]
        (let [{:keys [inherited default choices]
               :ui/keys [label]} option
              current-value (interface/get-raw-data context)
              value (or current-value
                        inherited
                        default)
              choice-map (options/choices->map choices)
              choice-name (get choice-map value)
              search-str @search
              filtered-choices (into []
                                     (keep (fn [[group-name & group]]
                                             (let [filtered-group (filter (fn [[display-name _]]
                                                                            (or (str/blank? search-str)
                                                                                (matches-search? display-name search-str)))
                                                                          group)]
                                               (when (seq filtered-group)
                                                 (into [group-name] filtered-group)))))
                                     choices)]
          [:div.ui-setting
           (when label
             [:label [tr label]])
           [:div.option
            [submenu/submenu context :string.option/select-colour-theme
             [:div
              [:div
               [tr choice-name]
               [value-mode-select/value-mode-select context]]
              [:div {:style {:transform "translate(-0.4em,0)"}}
               [theme-choice context value choice-name :clickable? false]]]
             {:class "image-select"
              :style {:width "22em"}}
             [:input.ui-submenu-search
              {:type "text"
               :placeholder (tr :string.miscellaneous/search)
               :value search-str
               :on-change #(reset! search (-> % .-target .-value))}]
             (into [:<>]
                   (map (fn [[group-name & group]]
                          (into
                           ^{:key group-name}
                           [:<>
                            [:h4 [tr group-name]]]
                           (map (fn [[display-name key]]
                                  ^{:key display-name}
                                  [theme-choice context key display-name :selected? (= key value)]))
                           group)))
                   filtered-choices)]]])))))
