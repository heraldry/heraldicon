(ns heraldicon.frontend.ui.form.ribbon
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.ui.element.submenu :as submenu]
   [heraldicon.frontend.ui.element.text-field :as text-field]
   [heraldicon.frontend.ui.interface :as ui.interface]
   [heraldicon.heraldry.ribbon :as ribbon]
   [heraldicon.interface :as interface]
   [heraldicon.localization.string :as string]))

(defn form [context]
  (ui.interface/form-elements
   context
   [:thickness
    :edge-angle
    :end-split
    :outline?]))

(defn- segment-form [context]
  (let [segment-type (interface/get-raw-data (c/++ context :type))
        idx (-> context :path last)
        z-index (interface/get-sanitized-data (c/++ context :z-index))
        title (string/str-tr (inc idx) ". "
                             (ribbon/segment-type-map segment-type)
                             ", layer " z-index)]

    [:div {:style {:position "relative"}}
     [submenu/submenu context nil [tr title] {:style {:width "28em"}
                                              :class "submenu-segment-form"}
      (ui.interface/form-elements
       context
       [:type
        :z-index
        :font
        :font-scale
        :spacing
        :offset-x
        :offset-y])]

     [text-field/text-field (c/++ context :text)
      :style {:display "inline-block"
              :position "absolute"
              :left "13em"}]]))

(defn segments-form [context & {:keys [tooltip]}]
  (let [num-segments (interface/get-list-size (c/++ context :segments))]
    [:div.option.ribbon-segments {:style {:margin-top "0.5em"}}
     [:div {:style {:font-size "1.3em"}} [tr :string.ribbon/segments]
      (when tooltip
        [:div.tooltip.info {:style {:display "inline-block"
                                    :margin-left "0.2em"
                                    :vertical-align "top"}}
         [:i.fas.fa-question-circle]
         [:div.bottom {:style {:width "20em"}}
          [tr tooltip]]])]

     (into [:ul]
           (map (fn [idx]
                  ^{:key idx}
                  [segment-form (c/++ context :segments idx)]))
           (range num-segments))

     [:p {:style {:color "#f86"}}
      [tr :string.ribbon.text/svg-font-rendering-warning]]]))
