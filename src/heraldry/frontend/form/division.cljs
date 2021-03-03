(ns heraldry.frontend.form.division
  (:require [heraldry.coat-of-arms.division.core :as division]
            [heraldry.coat-of-arms.division.options :as division-options]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.render :as render]
            [heraldry.frontend.form.element :as element]
            [heraldry.frontend.form.shared :as shared]
            [heraldry.frontend.form.state]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.util :as util]
            [re-frame.core :as rf]))

(defn division-choice [path key display-name]
  (let [division         (-> @(rf/subscribe [:get path])
                             :division)
        value            (-> division
                             :type
                             (or :none))
        {:keys [result]} (render/coat-of-arms
                          {:escutcheon :rectangle
                           :field      (if (= key :none)
                                         {:component :field
                                          :content   {:tincture (if (= value key) :or :azure)}}
                                         {:component :field
                                          :division  {:type   key
                                                      :fields (-> (division/default-fields {:type key})
                                                                  (util/replace-recursively :none :argent)
                                                                  (cond->
                                                                      (= value key) (util/replace-recursively :azure :or)))
                                                      :layout {:num-fields-x (case key
                                                                               :chequy    4
                                                                               :lozengy   3
                                                                               :vairy     2
                                                                               :potenty   2
                                                                               :papelonne 2
                                                                               nil)
                                                               :num-fields-y (case key
                                                                               :chequy    5
                                                                               :lozengy   4
                                                                               :vairy     3
                                                                               :potenty   3
                                                                               :papelonne 4
                                                                               nil)}}})}
                          100
                          (-> shared/coa-select-option-context
                              (assoc-in [:render-options :outline?] true)
                              (assoc-in [:render-options :theme] @(rf/subscribe [:get shared/ui-render-options-theme-path]))))]
    [:div.choice.tooltip {:on-click #(let [new-division              (assoc division :type key)
                                           {:keys [num-fields-x
                                                   num-fields-y
                                                   num-base-fields]} (:layout (options/sanitize-or-nil
                                                                               new-division
                                                                               (division-options/options new-division)))]
                                       (state/dispatch-on-event % [:set-division-type path key num-fields-x num-fields-y num-base-fields]))}
     [:svg {:style               {:width  "4em"
                                  :height "4.5em"}
            :viewBox             "0 0 120 200"
            :preserveAspectRatio "xMidYMin slice"}
      [:g {:filter "url(#shadow)"}
       [:g {:transform "translate(10,10)"}
        result]]]
     [:div.bottom
      [:h3 {:style {:text-align "center"}} display-name]
      [:i]]]))

(defn form [path]
  (let [division-type (-> @(rf/subscribe [:get path])
                          :division
                          :type
                          (or :none))
        names         (->> (into [["None" :none]]
                                 division/choices)
                           (map (comp vec reverse))
                           (into {}))]
    [:div.setting
     [:label "Division"]
     " "
     [element/submenu path "Select Division" (get names division-type) {:min-width "17.5em"}
      (for [[display-name key] (into [["None" :none]]
                                     division/choices)]
        ^{:key key}
        [division-choice path key display-name])]]))
