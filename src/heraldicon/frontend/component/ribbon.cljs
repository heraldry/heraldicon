(ns heraldicon.frontend.component.ribbon
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.element.core :as element]
   [heraldicon.frontend.element.submenu :as submenu]
   [heraldicon.frontend.element.text-field :as text-field]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.tooltip :as tooltip]
   [heraldicon.heraldry.ribbon :as ribbon]
   [heraldicon.interface :as interface]
   [heraldicon.localization.string :as string]))

(defn form [context]
  (element/elements
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
                             ", " :string.ribbon/layer " " z-index)]

    [:div {:style {:position "relative"}}
     [submenu/submenu context nil [tr title] {:style {:width "28em"}
                                              :class "submenu-segment-form"}
      (element/elements
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
      [tooltip/info tooltip :width "20em"]]

     (into [:ul]
           (map (fn [idx]
                  ^{:key idx}
                  [segment-form (c/++ context :segments idx)]))
           (range num-segments))

     [:p {:style {:color "#f86"}}
      [tr :string.ribbon.text/svg-font-rendering-warning]]]))
