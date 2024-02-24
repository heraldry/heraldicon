(ns heraldicon.frontend.component.field
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.blazonry-editor.core :as blazonry-editor]
   [heraldicon.frontend.component.core :as component]
   [heraldicon.frontend.component.element :as component.element]
   [heraldicon.frontend.component.field-component :as field-component]
   [heraldicon.frontend.element.arms-reference-select :as arms-reference-select]
   [heraldicon.frontend.element.core :as element]
   [heraldicon.frontend.element.tincture-select :as tincture-select]
   [heraldicon.frontend.macros :as macros]
   [heraldicon.frontend.repository.entity :as entity]
   [heraldicon.frontend.validation :as validation]
   [heraldicon.heraldry.default :as default]
   [heraldicon.heraldry.field.core :as field]
   [heraldicon.heraldry.tincture :as tincture]
   [heraldicon.interface :as interface]
   [heraldicon.static :as static]
   [re-frame.core :as rf]))

(defn- show-tinctures-only? [field-type]
  (-> field-type name keyword
      #{:chequy
        :lozengy
        :vairy
        :potenty
        :masony
        :papellony
        :fretty}))

(macros/reg-event-db ::load-arms
  (fn [db [_ path arms field-fn]]
    (let [{arms-id :id
           arms-version :version} arms
          field-fn (or field-fn identity)
          ;; TODO: this sets the data either right away when the result status is :done,
          ;; or inside the on-loaded call
          ;; this is a bit hacky still, also because it dispatches inside the event,
          ;; and there's a race condition, because the ::entity/data below also fetches
          ;; the arms with a different subscription, resulting in two requests being
          ;; sent to the API
          on-arms-load #(rf/dispatch [:set path (-> % :data :achievement :coat-of-arms :field field-fn)])
          {:keys [status entity]} @(rf/subscribe [::entity/data arms-id arms-version on-arms-load])]
      (when (= status :done)
        (on-arms-load entity))
      db)))

(defn- form [context]
  [:div {:style {:position "relative"}}
   (element/elements
    context
    [:inherit-environment?
     :adapt-to-ordinaries?
     :type
     :origin
     :anchor
     :orientation
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
                   [tincture-select/tincture-select (c/++ context :fields idx :field :tincture)]
                   [element/element (c/++ context :fields idx :field :pattern-scaling)]
                   [element/element (c/++ context :fields idx :field :pattern-rotation)]
                   [element/element (c/++ context :fields idx :field :pattern-offset-x)]
                   [element/element (c/++ context :fields idx :field :pattern-offset-y)]]))
           (range (interface/get-list-size (c/++ context :fields)))))

   [:div {:style {:position "absolute"
                  :top 0
                  :right 0}}
    [arms-reference-select/form context
     :title :string.option/load-from-arms
     :on-select #(rf/dispatch [::load-arms %1 %2])
     :display-selected-item? false
     :tooltip :string.tooltip/load-field-from-arms]]])

(defn- parent?
  [dragged-node-path drop-node-path]
  (= (drop-last 2 dragged-node-path)
     drop-node-path))

(defn drop-options-fn
  [dragged-node-path drop-node-path _drop-node-open?]
  (cond
    (field-component/inside-own-subtree?
     dragged-node-path drop-node-path) nil

    (not (field-component/component?
          dragged-node-path)) nil

    (parent?
     dragged-node-path
     drop-node-path) nil

    :else #{:inside}))

(defn drop-fn
  [dragged-node-context drop-node-context _where]
  (let [target-context (cond-> drop-node-context
                         (-> drop-node-context
                             :path
                             last
                             (not= :field)) (c/++ :field))]
    (rf/dispatch [::component.element/move-general
                  dragged-node-context
                  (c/++ target-context :components component.element/APPEND-INDEX)])))

(defmethod component/node :heraldry/field [context]
  (let [field-type (interface/get-raw-data (c/++ context :type))
        tincture (interface/get-sanitized-data (c/++ context :tincture))
        components-context (c/++ context :components)
        num-components (interface/get-list-size components-context)
        charge-preview? (-> context :path (= [:example-coa :coat-of-arms :field :components 0 :field]))]
    {:title (field/title context)
     :icon (case field-type
             :heraldry.field.type/plain (let [icon (tincture-select/preview
                                                    (tincture/pick tincture context)
                                                    (if (= tincture :none)
                                                      {:scale-x 5
                                                       :scale-y 6
                                                       :translate-x 0
                                                       :translate-y 0}
                                                      {:scale-x 10
                                                       :scale-y 10
                                                       :translate-x -15
                                                       :translate-y -40}))]
                                          {:default icon
                                           :selected icon})
             {:default (static/static-url
                        (str "/svg/field-type-" (name field-type) "-unselected.svg"))
              :selected (static/static-url
                         (str "/svg/field-type-" (name field-type) "-selected.svg"))})
     :validation (validation/validate-field context)
     :drop-options-fn drop-options-fn
     :drop-fn drop-fn
     :buttons (when-not charge-preview?
                [{:icon "fas fa-plus"
                  :title :string.button/add
                  :menu [{:title :string.entity/ordinary
                          :handler #(rf/dispatch [::component.element/add components-context default/ordinary])}
                         {:title :string.entity/charge
                          :handler #(rf/dispatch [::component.element/add components-context default/charge])}
                         {:title :string.entity/charge-group
                          :handler #(rf/dispatch [::component.element/add components-context default/charge-group])}
                         {:title :string.entity/semy
                          :handler #(rf/dispatch [::component.element/add components-context default/semy])}]}
                 {:icon "fas fa-pen-nib"
                  :title :string.button/from-blazon
                  :handler #(blazonry-editor/open context)}])
     :nodes (concat (when (and (not (show-tinctures-only? field-type))
                               (-> field-type name keyword (not= :plain)))
                      (let [fields-context (c/++ context :fields)
                            num-fields (interface/get-list-size fields-context)]
                        (->> (range num-fields)
                             (map (fn [idx]
                                    {:context (c/++ fields-context idx)}))
                             vec)))
                    (->> (range num-components)
                         (map (fn [idx]
                                (let [component-context (c/++ components-context idx)]
                                  {:context component-context
                                   :buttons [{:icon "fas fa-chevron-up"
                                              :disabled? (zero? idx)
                                              :title :string.tooltip/move-down
                                              :handler #(rf/dispatch [::component.element/move component-context (dec idx)])}
                                             {:icon "fas fa-chevron-down"
                                              :disabled? (= idx (dec num-components))
                                              :title :string.tooltip/move-up
                                              :handler #(rf/dispatch [::component.element/move component-context (inc idx)])}
                                             {:icon "far fa-trash-alt"
                                              :remove? true
                                              :title :string.tooltip/remove
                                              :handler #(rf/dispatch [::component.element/remove component-context])}]})))
                         vec))}))

(defmethod component/form :heraldry/field [_context]
  form)
