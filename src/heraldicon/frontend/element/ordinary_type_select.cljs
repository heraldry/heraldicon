(ns heraldicon.frontend.element.ordinary-type-select
  (:require
   [heraldicon.frontend.element.core :as element]
   [heraldicon.frontend.element.submenu :as submenu]
   [heraldicon.frontend.element.value-mode-select :as value-mode-select]
   [heraldicon.frontend.js-event :as js-event]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.macros :as macros]
   [heraldicon.frontend.tooltip :as tooltip]
   [heraldicon.heraldry.ordinary.options :as ordinary.options]
   [heraldicon.interface :as interface]
   [heraldicon.options :as options]
   [heraldicon.static :as static]
   [heraldicon.util.core :as util]
   [re-frame.core :as rf]))

(defn- default-line-style-of-ordinary-type [ordinary-type]
  (case ordinary-type
    :heraldry.ordinary.type/gore :enarched
    :straight))

(macros/reg-event-db ::set
  (fn [db [_ path new-type]]
    (let [current (get-in db path)
          has-default-line-style? (-> current
                                      :line
                                      :type
                                      (= (default-line-style-of-ordinary-type (:type current))))
          new-default-line-style (default-line-style-of-ordinary-type new-type)
          new-flipped (case new-type
                        :heraldry.ordinary.type/gore true
                        false)]
      (-> db
          (assoc-in (conj path :type) new-type)
          (cond->
            has-default-line-style? (update-in (conj path :line)
                                               assoc
                                               :type new-default-line-style
                                               :flipped? new-flipped))
          ;; TODO: the switch to context here is problematic
          (update-in path #(util/deep-merge-with (fn [_current-value new-value]
                                                   new-value)
                                                 %
                                                 (options/sanitize-or-nil % (interface/options {:path %}))))))))

(defn- ordinary-type-choice [path key display-name & {:keys [selected?
                                                             clickable?]
                                                      :or {clickable? true}}]
  (let [choice [:img.clickable {:style {:width "5em"
                                        :height "5.7em"}
                                :on-click (when clickable?
                                            (js-event/handled #(rf/dispatch [::set (vec (drop-last path)) key])))
                                :src (static/static-url
                                      (str "/svg/ordinary-type-" (name key) "-" (if selected? "selected" "unselected") ".svg"))}]]
    (if clickable?
      [tooltip/choice display-name choice]
      choice)))

(defmethod element/element :ui.element/ordinary-type-select [{:keys [path] :as context}]
  (when-let [option (interface/get-options context)]
    (let [current-value (interface/get-raw-data context)
          {:keys [inherited default choices]
           :ui/keys [label]} option
          value (or current-value
                    inherited
                    default)
          choice-map (options/choices->map choices)
          choice-name (get choice-map value)]
      [:div.ui-setting
       (when label
         [:label [tr label]])
       [:div.option
        [submenu/submenu context :string.option/select-ordinary
         [:div
          [:div
           [tr choice-name]
           [value-mode-select/value-mode-select context
            :display-fn ordinary.options/ordinary-map]]
          [:div {:style {:transform "translate(-0.42em,0)"}}
           [ordinary-type-choice path value choice-name :clickable? false]]]
         {:style {:width "21.5em"}}
         (into [:<>]
               (map (fn [[display-name key]]
                      ^{:key key}
                      [ordinary-type-choice path key display-name :selected? (= key value)]))
               choices)]]])))
