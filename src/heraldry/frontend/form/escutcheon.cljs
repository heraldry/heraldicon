(ns heraldry.frontend.form.escutcheon
  (:require [heraldry.coat-of-arms.escutcheon :as escutcheon]
            [heraldry.coat-of-arms.render :as render]
            [heraldry.frontend.form.element :as element]
            [heraldry.frontend.form.shared :as shared]
            [heraldry.frontend.form.state]
            [heraldry.frontend.state :as state]
            [re-frame.core :as rf]))

(defn escutcheon-choice [path key display-name]
  (let [value            @(rf/subscribe [:get path])
        {:keys [result]} (render/coat-of-arms
                          (if (= key :none)
                            {:escutcheon :rectangle
                             :field      {:type     :heraldry.field.type/plain
                                          :tincture :void}}
                            {:escutcheon key
                             :field      {:type     :heraldry.field.type/plain
                                          :tincture (if (= value key) :or :azure)}})
                          100
                          (-> shared/coa-select-option-context
                              (assoc-in [:render-options :outline?] true)
                              (assoc-in [:render-options :theme] @(rf/subscribe [:get shared/ui-render-options-theme-path]))))]
    [:div.choice.tooltip {:on-click #(state/dispatch-on-event % [:set path key])}
     [:svg {:style               {:width  "4em"
                                  :height "5em"}
            :viewBox             "0 0 120 200"
            :preserveAspectRatio "xMidYMin slice"}
      [:g {:filter "url(#shadow)"}
       [:g {:transform "translate(10,10)"}
        result]]]
     [:div.bottom
      [:h3 {:style {:text-align "center"}} display-name]
      [:i]]]))

(defn form [path label & {:keys [label-width allow-none? choices]}]
  (let [escutcheon (or @(rf/subscribe [:get path])
                       (when allow-none?
                         :none)
                       (when choices
                         (-> choices first second))
                       :heater)
        choices    (or choices
                       (if allow-none?
                         (concat [["None" :none]]
                                 escutcheon/choices)
                         escutcheon/choices))
        names      (->> choices
                        (map (comp vec reverse))
                        (into {}))]
    [:div.setting
     [:label label]
     " "
     (conj (if label-width
             [:div {:style {:display  "inline-block"
                            :position "absolute"
                            :left     label-width}}]
             [:<>])
           [element/submenu path "Select Escutcheon" (get names escutcheon) {:min-width "17.5em"}
            (for [[display-name key] choices]
              ^{:key key}
              [escutcheon-choice path key display-name])])
     [:div.spacer]]))

