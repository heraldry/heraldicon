(ns heraldicon.frontend.component.field
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.blazonry-editor.core :as blazonry-editor]
   [heraldicon.frontend.element.tincture-select :as tincture-select]
   [heraldicon.frontend.interface :as ui.interface]
   [heraldicon.frontend.macros :as macros]
   [heraldicon.frontend.state :as state]
   [heraldicon.frontend.validation :as validation]
   [heraldicon.heraldry.default :as default]
   [heraldicon.heraldry.field.core :as field]
   [heraldicon.heraldry.tincture :as tincture]
   [heraldicon.interface :as interface]
   [heraldicon.localization.string :as string]
   [heraldicon.static :as static]))

(macros/reg-event-db :override-field-part-reference
  (fn [db [_ path]]
    (let [{:keys [index]} (get-in db path)
          referenced-part (get-in db (-> path
                                         drop-last
                                         vec
                                         (conj index)))]
      (-> db
          (assoc-in path referenced-part)
          (state/ui-component-node-select path :open? true)))))

(macros/reg-event-db :reset-field-part-reference
  (fn [db [_ {:keys [path] :as context}]]
    (let [index (last path)
          parent-context (c/-- context 2)
          default-fields (field/default-fields parent-context)]
      (assoc-in db path (get default-fields index)))))

(defn- show-tinctures-only? [field-type]
  (-> field-type name keyword
      #{:chequy
        :lozengy
        :vairy
        :potenty
        :masony
        :papellony
        :fretty}))

(defn- form [context]
  [:<>
   (ui.interface/form-elements
    context
    [:inherit-environment?
     :type
     :tincture
     :pattern-scaling
     :pattern-rotation
     :pattern-offset-x
     :pattern-offset-y
     :line
     :opposite-line
     :extra-line
     :variant
     :thickness
     :gap
     :origin
     :anchor
     :orientation
     :geometry
     :layout
     :outline?
     :manual-blazon])

   (when (show-tinctures-only? (interface/get-raw-data (c/++ context :type)))
     (into [:<>
            [:div {:style {:margin-bottom "1em"}}]]
           (map (fn [idx]
                  ^{:key idx}
                  [:<>
                   [tincture-select/tincture-select (c/++ context :fields idx :tincture)]
                   [ui.interface/form-element (c/++ context :fields idx :pattern-scaling)]
                   [ui.interface/form-element (c/++ context :fields idx :pattern-rotation)]
                   [ui.interface/form-element (c/++ context :fields idx :pattern-offset-x)]
                   [ui.interface/form-element (c/++ context :fields idx :pattern-offset-y)]]))
           (range (interface/get-list-size (c/++ context :fields)))))])

(defn- parent-context [{:keys [path] :as context}]
  (let [index (last path)
        parent-context (c/-- context 2)
        parent-type (interface/get-raw-data (c/++ parent-context :type))]
    (when (and (int? index)
               (-> parent-type (or :dummy) namespace (= "heraldry.field.type")))
      parent-context)))

(defn- name-prefix-for-part [{:keys [path] :as context}]
  (when-let [parent-context (parent-context context)]
    (let [parent-type (interface/get-raw-data (c/++ parent-context :type))]
      (string/upper-case-first (field/part-name parent-type (last path))))))

(defn- non-mandatory-part-of-parent? [{:keys [path] :as context}]
  (let [index (last path)]
    (when (int? index)
      (when-let [parent-context (parent-context context)]
        (>= index (field/mandatory-part-count parent-context))))))

(defmethod ui.interface/component-node-data :heraldry/field [{:keys [path] :as context}]
  (let [field-type (interface/get-raw-data (c/++ context :type))
        ref? (= field-type :heraldry.field.type/ref)
        tincture (interface/get-sanitized-data (c/++ context :tincture))
        components-context (c/++ context :components)
        num-components (interface/get-list-size components-context)
        charge-preview? (-> context :path (= [:example-coa :coat-of-arms :field :components 0 :field]))]
    {:title (string/combine ": "
                            [(name-prefix-for-part context)
                             (if ref?
                               (string/str-tr :string.miscellaneous/field-reference
                                              " "
                                              (name-prefix-for-part
                                               (-> context
                                                   c/--
                                                   (c/++ (interface/get-raw-data
                                                          (c/++ context :index))))))
                               (field/title context))])
     :icon (case field-type
             :heraldry.field.type/plain (let [[scale-x
                                               scale-y
                                               translate-x
                                               translate-y] (if (= tincture :none)
                                                              [5 6 0 0]
                                                              [10 10 -15 -40])
                                              mask-id "preview-mask"
                                              icon [:svg {:version "1.1"
                                                          :xmlns "http://www.w3.org/2000/svg"
                                                          :xmlnsXlink "http://www.w3.org/1999/xlink"
                                                          :viewBox (str "0 0 120 140")
                                                          :preserveAspectRatio "xMidYMin slice"}
                                                    [:g {:transform "translate(10,10)"}
                                                     [:mask {:id mask-id}
                                                      [:rect {:x 0
                                                              :y 0
                                                              :width 100
                                                              :height 120
                                                              :stroke "none"
                                                              :fill "#fff"}]]
                                                     [:g {:mask (str "url(#" mask-id ")")}
                                                      [:g {:transform (str "translate(" translate-x "," translate-y ")")}
                                                       [:rect {:x 0
                                                               :y 0
                                                               :width 100
                                                               :height 120
                                                               :stroke "none"
                                                               :fill (tincture/pick tincture context)
                                                               :transform (str "scale(" scale-x "," scale-y ")")}]]]
                                                     [:rect {:x 0
                                                             :y 0
                                                             :width 100
                                                             :height 120
                                                             :stroke "#000"
                                                             :fill "none"}]]]]
                                          {:default icon
                                           :selected icon})
             :heraldry.field.type/ref {:default [:span {:style {:display "inline-block"}}]
                                       :selected [:span {:style {:display "inline-block"}}]}
             {:default (static/static-url
                        (str "/svg/field-type-" (name field-type) "-unselected.svg"))
              :selected (static/static-url
                         (str "/svg/field-type-" (name field-type) "-selected.svg"))})
     :validation (validation/validate-field context)
     :buttons (when-not charge-preview?
                (if ref?
                  [{:icon "fas fa-sliders-h"
                    :title :string.user.button/change
                    :handler #(state/dispatch-on-event % [:override-field-part-reference path])}]
                  (cond-> [{:icon "fas fa-plus"
                            :title :string.button/add
                            :menu [{:title :string.entity/ordinary
                                    :handler #(state/dispatch-on-event % [:add-element components-context default/ordinary])}
                                   {:title :string.entity/charge
                                    :handler #(state/dispatch-on-event % [:add-element components-context default/charge])}
                                   {:title :string.entity/charge-group
                                    :handler #(state/dispatch-on-event % [:add-element components-context default/charge-group])}
                                   {:title :string.entity/semy
                                    :handler #(state/dispatch-on-event % [:add-element components-context default/semy])}]}
                           {:icon "fas fa-pen-nib"
                            :title :string.button/from-blazon
                            :handler #(blazonry-editor/open context)}]
                    (non-mandatory-part-of-parent? context)
                    (conj {:icon "fas fa-undo"
                           :title "Reset"
                           :handler #(state/dispatch-on-event % [:reset-field-part-reference context])}))))
     :nodes (concat (when (and (not (show-tinctures-only? field-type))
                               (-> field-type name keyword (not= :plain)))
                      (let [fields-context (c/++ context :fields)
                            num-fields (interface/get-list-size fields-context)]
                        (->> (range num-fields)
                             (map (fn [idx]
                                    {:context (c/++ fields-context idx)}))
                             vec)))
                    (->> (range num-components)
                         reverse
                         (map (fn [idx]
                                (let [component-context (c/++ components-context idx)]
                                  {:context component-context
                                   :buttons [{:icon "fas fa-chevron-down"
                                              :disabled? (zero? idx)
                                              :title :string.tooltip/move-down
                                              :handler #(state/dispatch-on-event % [:move-element component-context (dec idx)])}
                                             {:icon "fas fa-chevron-up"
                                              :disabled? (= idx (dec num-components))
                                              :title :string.tooltip/move-up
                                              :handler #(state/dispatch-on-event % [:move-element component-context (inc idx)])}
                                             {:icon "far fa-trash-alt"
                                              :remove? true
                                              :title :string.tooltip/remove
                                              :handler #(state/dispatch-on-event % [:remove-element component-context])}]})))
                         vec))}))

(defmethod ui.interface/component-form-data :heraldry/field [_context]
  {:form form})