(ns heraldry.frontend.form.collection
  (:require [heraldry.frontend.form.arms-reference :as arms-reference]
            [heraldry.frontend.form.element :as element]
            [heraldry.frontend.form.font :as font]
            [heraldry.frontend.state :as state]
            [re-frame.core :as rf]))

(defn collection-element [path]
  (let [index (last path)
        data @(rf/subscribe [:get path])
        element-name (-> data
                         :name
                         (or "None"))]
    [element/component path :collection-element (str (inc index) ": " element-name) nil
     [:div.setting {:style {:margin-bottom "1em"}}
      [element/text-field (conj path :name) "Name" :style {:width "14em"}]]

     [arms-reference/form (conj path :reference)]]))

(defn form [path selected-arms-path]
  (let [data @(rf/subscribe [:get path])
        selected-arms @(rf/subscribe [:get selected-arms-path])]
    [element/component path :collection (:name data) "Collection"
     [:div.setting {:style {:margin-bottom "1em"}}
      [font/form (conj path :font)]]

     [:div.settings
      [element/range-input (conj path :collection :num-columns) "Columns"
       1
       10
       :default 6]]

     (when selected-arms
       (let [element-path (conj path :collection :elements selected-arms)]
         [:div.selected-arms
          [:div.no-select {:style {:padding-right "10px"
                                   :white-space "nowrap"}}

           [:a (if (zero? selected-arms)
                 {:class "disabled"}
                 {:on-click #(do
                               (rf/dispatch [:set selected-arms-path (dec selected-arms)])
                               (state/dispatch-on-event % [:move-element-down element-path]))})
            [:i.fas.fa-chevron-down]]
           " "
           [:a (if (= selected-arms (-> data :collection :elements count dec))
                 {:class "disabled"}
                 {:on-click #(do
                               (rf/dispatch [:set selected-arms-path (inc selected-arms)])
                               (state/dispatch-on-event % [:move-element-up element-path]))})
            [:i.fas.fa-chevron-up]]]

          [collection-element element-path]

          [:div {:style {:padding-left "10px"}}
           [:a {:on-click #(do
                             (rf/dispatch [:set selected-arms-path nil])
                             (state/dispatch-on-event % [:remove-element element-path]))}
            [:i.far.fa-trash-alt]]]]))]))
