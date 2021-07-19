(ns heraldry.frontend.ui.element.ordinary-type-select
  (:require [heraldry.coat-of-arms.ordinary.options :as ordinary-options]
            [heraldry.coat-of-arms.render :as render]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.ui.element.submenu :as submenu]
            [heraldry.frontend.ui.element.value-mode-select :as value-mode-select]
            [heraldry.frontend.ui.interface :as interface]
            [heraldry.frontend.ui.shared :as shared]
            [heraldry.options :as options]
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
  (let [{:keys [result]} (render/coat-of-arms
                          [:coat-of-arms]
                          100
                          (-> shared/coa-select-option-context
                              (assoc-in [:data :coat-of-arms]
                                        {:escutcheon :rectangle
                                         :field {:type :heraldry.field.type/plain
                                                 :tincture :argent
                                                 :components [{:type key
                                                               :line (when (= key :heraldry.ordinary.type/gore)
                                                                       {:type :enarched
                                                                        :flipped? true})
                                                               :origin (case key
                                                                         :heraldry.ordinary.type/label {:alignment :left}
                                                                         nil)
                                                               :geometry (case key
                                                                           :heraldry.ordinary.type/label {:width 75
                                                                                                          :size 12
                                                                                                          :thickness 20}
                                                                           :heraldry.ordinary.type/pile {:stretch 0.85}
                                                                           nil)
                                                               :field {:type :heraldry.field.type/plain
                                                                       :tincture (if selected? :or :azure)}}]}})))]
    [:div.choice.tooltip {:on-click #(state/dispatch-on-event % [:set-ordinary-type (vec (drop-last path)) key])}
     [:svg {:style {:width "4em"
                    :height "4.5em"}
            :viewBox "0 0 120 200"
            :preserveAspectRatio "xMidYMin slice"}
      [:g {:transform "translate(10,10)"}
       result]]
     [:div.bottom
      [:h3 {:style {:text-align "center"}} display-name]
      [:i]]]))

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
        [submenu/submenu path "Select Ordinary" (get ordinary-options/ordinary-map value) {:width "21.5em"}
         (for [[display-name key] choices]
           ^{:key key}
           [ordinary-type-choice path key display-name :selected? (= key value)])]
        [value-mode-select/value-mode-select path
         :display-fn ordinary-options/ordinary-map]]])))

(defmethod interface/form-element :ordinary-type-select [path]
  [ordinary-type-select path])
