(ns heraldicon.frontend.element.charge-type-select
  (:require
   [heraldicon.context :as c]
   [heraldicon.entity.id :as id]
   [heraldicon.frontend.element.charge-select :as charge-select]
   [heraldicon.frontend.element.core :as element]
   [heraldicon.frontend.element.submenu :as submenu]
   [heraldicon.frontend.entity.preview :as preview]
   [heraldicon.frontend.js-event :as js-event]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.macros :as macros]
   [heraldicon.frontend.tooltip :as tooltip]
   [heraldicon.heraldry.charge.options :as charge.options]
   [heraldicon.interface :as interface]
   [heraldicon.static :as static]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as reife]))

(macros/reg-event-db ::update
  (fn [db [_ path changes]]
    (update-in db path merge changes)))

(defn- charge-type-choice [path key display-name & {:keys [selected?]}]
  (let [choice [:img.clickable {:style {:width "4em"
                                        :height "4.5em"}
                                :on-click (js-event/handled
                                           #(rf/dispatch [::update path {:type key
                                                                         :attitude nil
                                                                         :facing nil
                                                                         :data nil
                                                                         :variant nil}]))
                                :src (static/static-url
                                      (str "/svg/charge-type-" (name key) "-" (if selected? "selected" "unselected") ".svg"))}]]
    [tooltip/choice display-name choice]))

(defn choice-preview-url [context]
  (if (interface/get-raw-data (c/++ context :preview?))
    (static/static-url
     (str "/svg/charge-type-roundel-unselected.svg"))
    (let [charge-type-context (c/++ context :type)
          variant-context (c/++ context :variant)
          {:keys [inherited default]} (interface/get-relevant-options charge-type-context)
          current-value (interface/get-raw-data charge-type-context)
          value (or current-value
                    inherited
                    default)
          variant (interface/get-raw-data variant-context)]
      (if variant
        (preview/url
         :charge variant
         :width 100
         :height 120)
        (static/static-url
         (str "/svg/charge-type-" (name value) "-unselected.svg"))))))

(defn- choice-preview [context]
  (let [variant-context (c/++ context :variant)
        variant (interface/get-raw-data variant-context)
        img-url (choice-preview-url context)]
    [:div {:style {:transform (when (not variant)
                                "translate(-0.333em,0)")
                   :position "relative"
                   :width "4em"
                   :height "4.5em"
                   :border (when variant
                             "1.5px solid #ddd")}}
     [:img.clickable {:src img-url
                      :style {:position "absolute"
                              :margin "auto"
                              :top 0
                              :left 0
                              :right 0
                              :bottom 0
                              :max-width "100%"
                              :max-height "100%"}}]]))

(defmethod element/element :ui.element/charge-type-select [context]
  (when-let [option (interface/get-relevant-options context)]
    (let [charge-context (c/-- context)
          variant-context (c/++ charge-context :variant)
          variant (interface/get-raw-data variant-context)
          current-value (interface/get-raw-data context)
          {:keys [inherited default choices]
           :ui/keys [label]} option
          value (or current-value
                    inherited
                    default)]
      [:div.ui-setting
       (when label
         [:label [tr label]])
       [:div.option
        [submenu/submenu context :string.option/select-charge
         [:div
          [tr (charge.options/title charge-context)]
          [choice-preview charge-context]]
         {:style {:position "fixed"
                  :transform "none"
                  :left "45vw"
                  :width "53vw"
                  :top "10vh"
                  :height "80vh"}}
         [:div
          (into [:<>]
                (map (fn [[display-name key]]
                       ^{:key key}
                       [charge-type-choice (:path charge-context) key display-name :selected? (and (= key value)
                                                                                                   (not variant))]))
                choices)
          [charge-select/list-charges
           (fn [{:keys [id version data]}]
             {:href (reife/href :route.charge.details/by-id {:id (id/for-url id)})
              :on-click (fn [event]
                          (doto event
                            .preventDefault
                            .stopPropagation)
                          (rf/dispatch [::update
                                        (:path charge-context)
                                        (merge {:type (->> data
                                                           :charge-type
                                                           name
                                                           (keyword "heraldry.charge.type"))
                                                :variant {:id id
                                                          :version version}
                                                :attitude nil
                                                :facing nil}
                                               (select-keys data [:attitude :facing]))]))})
           :selected-item variant
           :favour-heraldicon? true
           :initial-sorting-mode :name
           :display-selected-item? true
           :default-list-mode :small
           :list-id :charge-type-select]]]]])))
