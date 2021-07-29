(ns heraldry.frontend.ui.element.ordinary-type-select
  (:require [heraldry.coat-of-arms.ordinary.options :as ordinary-options]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.ui.element.submenu :as submenu]
            [heraldry.frontend.ui.element.value-mode-select :as value-mode-select]
            [heraldry.frontend.ui.interface :as interface]
            [heraldry.options :as options]
            [heraldry.static :as static]
            [heraldry.util :as util]
            [re-frame.core :as rf]))

(defn -default-line-style-of-ordinary-type [ordinary-type]
  (case ordinary-type
    :heraldry.ordinary.type/gore :enarched
    :straight))

(rf/reg-event-db :set-ordinary-type
  (fn [db [_ path new-type]]
    (let [current (get-in db path)
          has-default-line-style? (-> current
                                      :line
                                      :type
                                      (= (-default-line-style-of-ordinary-type (:type current))))
          new-default-line-style (-default-line-style-of-ordinary-type new-type)
          new-flipped (case new-type
                        :heraldry.ordinary.type/gore true
                        false)]
      (-> db
          (assoc-in (conj path :type) new-type)
          (cond->
           has-default-line-style? (->
                                    (assoc-in (conj path :line :type) new-default-line-style)
                                    (assoc-in (conj path :line :flipped?) new-flipped)))
          (update-in path #(util/deep-merge-with (fn [_current-value new-value]
                                                   new-value)
                                                 %
                                                 (options/sanitize-or-nil % (ordinary-options/options %))))))))

(defn ordinary-type-choice [path key display-name & {:keys [selected?]}]
  [:div.choice.tooltip {:on-click #(state/dispatch-on-event % [:set-ordinary-type (vec (drop-last path)) key])}
   [:img.clickable {:style {:width "5em"
                            :height "5.7em"}
                    :src (static/static-url
                          (str "/svg/ordinary-type-" (name key) "-" (if selected? "selected" "unselected") ".svg"))}]
   [:div.bottom
    [:h3 {:style {:text-align "center"}} display-name]
    [:i]]])

(defn ordinary-type-select [path]
  (when-let [option @(rf/subscribe [:get-relevant-options path])]
    (let [current-value @(rf/subscribe [:get-value path])
          {:keys [ui inherited default choices]} option
          value (or current-value
                    inherited
                    default)
          label (:label ui)]
      [:div.ui-setting
       (when label
         [:label label])
       [:div.option
        [submenu/submenu path "Select Ordinary" (get ordinary-options/ordinary-map value) {:style {:width "21.5em"}}
         (for [[display-name key] choices]
           ^{:key key}
           [ordinary-type-choice path key display-name :selected? (= key value)])]
        [value-mode-select/value-mode-select path
         :display-fn ordinary-options/ordinary-map]]])))

(defmethod interface/form-element :ordinary-type-select [path]
  [ordinary-type-select path])
