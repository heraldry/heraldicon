(ns heraldry.frontend.ui.element.attributes
  (:require [heraldry.coat-of-arms.attributes :as attributes]
            [heraldry.frontend.ui.interface :as interface]
            [heraldry.util :as util]
            [re-frame.core :as rf]))

(defn attribute-view [attribute & {:keys [on-delete]}]
  [:span.tag.attribute
   (name attribute)
   (when on-delete
     [:span.delete {:on-click on-delete}
      "x"])])

(defn attributes-view [attributes & {:keys [on-delete
                                            on-click
                                            selected]}]
  [:div.attributes
   (for [attribute (sort attributes)]
     ^{:key attribute}
     [:<>
      [attribute-view attribute
       :on-delete (when on-delete
                    #(on-delete attribute))
       :on-click (when on-click
                   #(on-click attribute))
       :selected? (get selected attribute)]
      " "])])

(defn form [path]
  (let [attributes @(rf/subscribe [:get-value path])]
    [:<>
     [:div.ui-setting {:style {:margin-top "10px"
                               :white-space "nowrap"}}
      [:label "Attributes"]
      [:div.option
       [:select {:on-change #(let [selected (keyword (-> % .-target .-value))]
                               (rf/dispatch [:add-attribute path selected]))
                 :value :none}
        (for [[group-name & group-choices] (concat
                                            [["--- Add attribute ---" :none]]
                                            attributes/attribute-choices)]
          (if (and (-> group-choices count (= 1))
                   (-> group-choices first keyword?))
            (let [key (-> group-choices first)]
              ^{:key key}
              [:option {:value (util/keyword->str key)} group-name])
            ^{:key group-name}
            [:optgroup {:label group-name}
             (for [[display-name key] group-choices]
               ^{:key key}
               [:option {:value (util/keyword->str key)} display-name])]))]
       [:div {:style {:padding-top "10px"}}
        [attributes-view (keys attributes)
         :on-delete #(rf/dispatch [:remove-attribute path %])]]]]]))

(defmethod interface/form-element :attributes [path]
  [form path])
