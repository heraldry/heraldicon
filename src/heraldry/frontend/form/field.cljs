(ns heraldry.frontend.form.field
  (:require [heraldry.coat-of-arms.default :as default]
            [heraldry.coat-of-arms.field.core :as field]
            [heraldry.coat-of-arms.field.options :as field-options]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.render :as render]
            [heraldry.frontend.form.charge :as charge]
            [heraldry.frontend.form.charge-group :as charge-group]
            [heraldry.frontend.form.element :as element]
            [heraldry.frontend.form.geometry :as geometry]
            [heraldry.frontend.form.line :as line]
            [heraldry.frontend.form.ordinary :as ordinary]
            [heraldry.frontend.form.position :as position]
            [heraldry.frontend.form.semy :as semy]
            [heraldry.frontend.form.shared :as shared]
            heraldry.frontend.form.state
            [heraldry.frontend.form.tincture :as tincture]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.util :as util]
            [re-frame.core :as rf]))

(defn form-for-plain-field [path]
  [:div.form-plain-field
   [tincture/form (conj path :tincture)]])

(defn field-type-choice [path key display-name]
  (let [field @(rf/subscribe [:get path])
        value (:type field)
        {:keys [result]} (render/coat-of-arms
                          {:escutcheon :rectangle
                           :field (if (= key :heraldry.field.type/plain)
                                    {:type :heraldry.field.type/plain
                                     :tincture (if (= value key) :or :azure)}
                                    {:type key
                                     :fields (-> (field/default-fields {:type key})
                                                 (util/replace-recursively :none :argent)
                                                 (cond->
                                                  (= value key) (util/replace-recursively :azure :or)))
                                     :layout {:num-fields-x (case key
                                                              :heraldry.field.type/chequy 4
                                                              :heraldry.field.type/lozengy 3
                                                              :heraldry.field.type/vairy 2
                                                              :heraldry.field.type/potenty 2
                                                              :heraldry.field.type/papellony 2
                                                              :heraldry.field.type/masonry 2
                                                              nil)
                                              :num-fields-y (case key
                                                              :heraldry.field.type/chequy 5
                                                              :heraldry.field.type/lozengy 4
                                                              :heraldry.field.type/vairy 3
                                                              :heraldry.field.type/potenty 3
                                                              :heraldry.field.type/papellony 4
                                                              :heraldry.field.type/masonry 4
                                                              nil)}})}
                          100
                          (-> shared/coa-select-option-context
                              (assoc-in [:render-options :outline?] true)
                              (assoc-in [:render-options :theme] @(rf/subscribe [:get shared/ui-render-options-theme-path]))))]
    [:div.choice.tooltip {:on-click #(let [new-field (assoc field :type key)
                                           {:keys [num-fields-x
                                                   num-fields-y
                                                   num-base-fields]} (:layout (options/sanitize-or-nil
                                                                               new-field
                                                                               (field-options/options new-field)))]
                                       (state/dispatch-on-event % [:set-field-type path key num-fields-x num-fields-y num-base-fields]))}
     [:svg {:style {:width "4em"
                    :height "4.5em"}
            :viewBox "0 0 120 200"
            :preserveAspectRatio "xMidYMin slice"}
      [:g {:filter "url(#shadow)"}
       [:g {:transform "translate(10,10)"}
        result]]]
     [:div.bottom
      [:h3 {:style {:text-align "center"}} display-name]
      [:i]]]))

(defn field-type-form [path]
  (let [field-type (-> @(rf/subscribe [:get path])
                       :type)
        names (->> field/choices
                   (map (comp vec reverse))
                   (into {}))]
    [:div.setting
     [:label "Division"]
     " "
     [element/submenu path "Select Division" (get names field-type) {:min-width "17.5em"}
      (for [[display-name key] field/choices]
        ^{:key key}
        [field-type-choice path key display-name])]]))

(defn form-for-layout [field-path & {:keys [title options] :or {title "Layout"}}]
  (let [layout-path (conj field-path :layout)
        field @(rf/subscribe [:get field-path])
        layout (:layout field)
        field-type (:type field)
        stripped-field-type (-> field-type name keyword)
        current-data (options/sanitize-or-nil field (field-options/options field))
        effective-data (:layout (options/sanitize field (field-options/options field)))
        link-name (util/combine
                   ", "
                   [(cond
                      (= stripped-field-type :paly) (str (:num-fields-x effective-data) " fields")
                      (#{:barry
                         :bendy
                         :bendy-sinister} stripped-field-type) (str (:num-fields-y effective-data) " fields"))
                    (when (and (:num-base-fields current-data)
                               (not= (:num-base-fields current-data) 2))
                      (str (:num-base-fields effective-data) " base fields"))
                    (when (or (:offset-x current-data)
                              (:offset-y current-data))
                      (str "shifted"))
                    (when (or (:stretch-x current-data)
                              (:stretch-y current-data))
                      (str "stretched"))
                    (when (:rotation current-data)
                      (str "rotated"))])
        link-name (if (-> link-name count (= 0))
                    "Default"
                    link-name)]
    [:div.setting
     [:label title]
     " "
     [element/submenu layout-path title link-name {}
      (when (-> options :num-base-fields)
        [element/range-input-with-checkbox (conj layout-path :num-base-fields) "Base fields"
         (-> options :num-base-fields :min)
         (-> options :num-base-fields :max)
         :default (options/get-value (:num-base-fields layout) (:num-base-fields options))
         :on-change (fn [value]
                      (rf/dispatch [:set-field-type
                                    field-path
                                    field-type
                                    (:num-fields-x layout)
                                    (:num-fields-y layout)
                                    value]))])
      (when (-> options :num-fields-x)
        [element/range-input-with-checkbox (conj layout-path :num-fields-x) "x-Subfields"
         (-> options :num-fields-x :min)
         (-> options :num-fields-x :max)
         :default (options/get-value (:num-fields-x layout) (:num-fields-x options))
         :on-change (fn [value]
                      (rf/dispatch [:set-field-type
                                    field-path
                                    field-type
                                    value
                                    (:num-fields-y layout)
                                    (:num-base-fields layout)]))])
      (when (-> options :num-fields-y)
        [element/range-input-with-checkbox (conj layout-path :num-fields-y) "y-Subfields"
         (-> options :num-fields-y :min)
         (-> options :num-fields-y :max)
         :default (options/get-value (:num-fields-y layout) (:num-fields-y options))
         :on-change (fn [value]
                      (rf/dispatch [:set-field-type
                                    field-path
                                    field-type
                                    (:num-fields-x layout)
                                    value
                                    (:num-base-fields layout)]))])
      (when (-> options :offset-x)
        [element/range-input-with-checkbox (conj layout-path :offset-x) "Offset x"
         (-> options :offset-x :min)
         (-> options :offset-x :max)
         :step 0.01
         :default (options/get-value (:offset-x layout) (:offset-x options))])
      (when (-> options :offset-y)
        [element/range-input-with-checkbox (conj layout-path :offset-y) "Offset y"
         (-> options :offset-y :min)
         (-> options :offset-y :max)
         :step 0.01
         :default (options/get-value (:offset-y layout) (:offset-y options))])
      (when (-> options :stretch-x)
        [element/range-input-with-checkbox (conj layout-path :stretch-x) "Stretch x"
         (-> options :stretch-x :min)
         (-> options :stretch-x :max)
         :step 0.01
         :default (options/get-value (:stretch-x layout) (:stretch-x options))])
      (when (-> options :stretch-y)
        [element/range-input-with-checkbox (conj layout-path :stretch-y) "Stretch y"
         (-> options :stretch-y :min)
         (-> options :stretch-y :max)
         :step 0.01
         :default (options/get-value (:stretch-y layout) (:stretch-y options))])
      (when (-> options :rotation)
        [element/range-input-with-checkbox (conj layout-path :rotation) "Rotation"
         (-> options :rotation :min)
         (-> options :rotation :max)
         :step 5
         :default (options/get-value (:rotation layout) (:rotation options))])]]))

(defn form [path & {:keys [parent-field title-prefix]}]
  (let [field @(rf/subscribe [:get path])
        field-type (:type field)
        counterchanged? @(rf/subscribe [:get (conj path :counterchanged?)])
        root-field? (= path [:coat-of-arms :field])]
    [element/component path :field (cond
                                     (:counterchanged? field) "Counterchanged"
                                     (= field-type :heraldry.field.type/plain) (-> field :tincture util/translate-tincture util/upper-case-first)
                                     :else (-> field-type util/translate-cap-first)) title-prefix
     [:div.settings
      (when (not root-field?)
        [element/checkbox (conj path :inherit-environment?) "Inherit environment (dimidiation)"])
      (when (and (not= path [:coat-of-arms :field])
                 parent-field)
        [element/checkbox (conj path :counterchanged?) "Counterchanged"])
      (when (not counterchanged?)
        [:<>
         [field-type-form path]
         (let [options (field-options/options field)]
           [:<>
            (when (:line options)
              [line/form (conj path :line) :options (:line options)])
            (when (:opposite-line options)
              [line/form (conj path :opposite-line)
               :options (:opposite-line options)
               :defaults (options/sanitize (:line field) (:line options))
               :title "Opposite Line"])
            (when (:extra-line options)
              [line/form (conj path :extra-line)
               :options (:extra-line options)
               :defaults (options/sanitize (:line field) (:line options))
               :title "Extra Line"])
            (when (-> options :variant)
              [element/select (conj path :variant) "Variant"
               (-> options :variant :choices)
               :default (-> options :variant :default)])
            (when (-> options :thickness)
              [element/range-input-with-checkbox (conj path :thickness) "Thickness"
               (-> options :thickness :min)
               (-> options :thickness :max)
               :step 0.01
               :default (-> options :thickness :default)])
            (when (-> options :origin)
              [position/form (conj path :origin)
               :title "Origin"
               :options (:origin options)])
            (when (:direction-anchor options)
              [position/form (conj path :direction-anchor)
               :title "Issuant"
               :options (:direction-anchor options)])
            (when (-> options :anchor)
              [position/form (conj path :anchor)
               :title "Anchor"
               :options (:anchor options)])
            (when (:geometry options)
              [geometry/form (conj path :geometry)
               (:geometry options)
               :current (:geometry field)])
            (when (:layout options)
              [form-for-layout path :options (:layout options)])])
         (if (= field-type :heraldry.field.type/plain)
           [form-for-plain-field path]
           [element/checkbox (conj path :outline?) "Outline"])])]
     (cond
       (#{:heraldry.field.type/chequy
          :heraldry.field.type/lozengy
          :heraldry.field.type/vairy
          :heraldry.field.type/potenty
          :heraldry.field.type/papellony
          :heraldry.field.type/masonry} field-type) [:div.parts.components {:style {:margin-bottom "0.5em"}}
                                                     [:ul
                                                      (let [tinctures @(rf/subscribe [:get (conj path :fields)])]
                                                        (for [idx (range (count tinctures))]
                                                          ^{:key idx}
                                                          [:li
                                                           [tincture/form (conj path :fields idx :tincture)
                                                            :label (str "Tincture " (inc idx))]]))]]
       (and (not counterchanged?)
            (not= field-type :heraldry.field.type/plain)) [:div.parts.components
                                                           [:ul
                                                            (let [content @(rf/subscribe [:get (conj path :fields)])
                                                                  mandatory-part-count (field/mandatory-part-count field)]
                                                              (for [[idx part] (map-indexed vector content)]
                                                                (let [part-path (conj path :fields idx)
                                                                      part-name (field/part-name field-type idx)
                                                                      ref (when (-> part
                                                                                    :type
                                                                                    (= :heraldry.field.type/ref))
                                                                            (:index part))]
                                                                  ^{:key idx}
                                                                  [:li
                                                                   [:div
                                                                    (if ref
                                                                      [element/component part-path :ref (str "Same as " (field/part-name field-type ref)) part-name]
                                                                      [form part-path :title-prefix part-name])]
                                                                   [:div {:style {:padding-left "10px"}}
                                                                    (if ref
                                                                      [:a {:on-click #(do (state/dispatch-on-event % [:set part-path (get content ref)])
                                                                                          (state/dispatch-on-event % [:ui-component-open part-path]))}
                                                                       [:i.far.fa-edit]]
                                                                      (when (>= idx mandatory-part-count)
                                                                        [:a {:on-click #(state/dispatch-on-event % [:set part-path
                                                                                                                    (-> (field/default-fields field)
                                                                                                                        (get idx))])}
                                                                         [:i.far.fa-times-circle]]))]])))]])
     [:div {:style {:margin-bottom "0.5em"}}
      [:button {:on-click #(state/dispatch-on-event % [:add-component path default/ordinary])}
       [:i.fas.fa-plus] " Ordinary"]
      " "
      [:button {:on-click #(state/dispatch-on-event % [:add-component path default/charge])}
       [:i.fas.fa-plus] " Charge"]
      " "
      [:button {:on-click #(state/dispatch-on-event % [:add-component path default/charge-group])}
       [:i.fas.fa-plus] " Charge group"]
      " "
      [:button {:on-click #(state/dispatch-on-event % [:add-component path default/semy])}
       [:i.fas.fa-plus] " Semy"]]
     [:div.components
      [:ul
       (let [components @(rf/subscribe [:get (conj path :components)])]
         (for [[idx component] (reverse (map-indexed vector components))]
           (let [component-path (conj path :components idx)]
             ^{:key idx}
             [:li
              [:div.no-select {:style {:padding-right "10px"
                                       :white-space "nowrap"}}
               [:a (if (zero? idx)
                     {:class "disabled"}
                     {:on-click #(state/dispatch-on-event % [:move-element-down component-path])})
                [:i.fas.fa-chevron-down]]
               " "
               [:a (if (= idx (dec (count components)))
                     {:class "disabled"}
                     {:on-click #(state/dispatch-on-event % [:move-element-up component-path])})
                [:i.fas.fa-chevron-up]]]
              [:div
               (case (-> component :type namespace)
                 "heraldry.ordinary.type" [ordinary/form component-path
                                           :parent-field field
                                           :form-for-field form]
                 "heraldry.charge.type" [charge/form component-path
                                         :parent-field field
                                         :form-for-field form]
                 "heraldry.charge-group.type" [charge-group/form component-path
                                               :parent-field field
                                               :form-for-field form]
                 "heraldry.component" [semy/form component-path
                                       :parent-field field
                                       :form-for-layout form-for-layout
                                       :form-for-field form])]
              [:div {:style {:padding-left "10px"}}
               (when (not (and (some-> component :type namespace (= "heraldry.charge.type"))
                               (-> component :data)))
                 [:a {:on-click #(state/dispatch-on-event % [:remove-element component-path])}
                  [:i.far.fa-trash-alt]])]])))]]]))
