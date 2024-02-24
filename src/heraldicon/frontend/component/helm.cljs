(ns heraldicon.frontend.component.helm
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.component.core :as component]
   [heraldicon.frontend.component.element :as component.element]
   [heraldicon.frontend.component.field-component :as field-component]
   [heraldicon.heraldry.default :as default]
   [heraldicon.heraldry.shield-separator :as shield-separator]
   [heraldicon.interface :as interface]
   [heraldicon.localization.string :as string]
   [re-frame.core :as rf]))

(rf/reg-sub ::status
  (fn [[_ path] _]
    (rf/subscribe [:get (conj path :components)]))

  (fn [components [_ _path]]
    {:helmet? (->> components
                   (filter (fn [component]
                             (-> component
                                 :function
                                 (= :heraldry.charge.function/helmet))))
                   seq)
     :torse? (->> components
                  (filter (fn [component]
                            (-> component
                                :function
                                (= :heraldry.charge.function/torse))))
                  seq)}))

(defn parent?
  [dragged-node-path drop-node-path]
  (= (drop-last 2 dragged-node-path)
     drop-node-path))

(defn drop-options-fn
  [dragged-node-path dragged-node-type
   drop-node-path _drop-node-type
   drop-node-open?]
  (cond-> (cond
            (field-component/inside-own-subtree?
             dragged-node-path
             drop-node-path) nil

            (parent?
             dragged-node-path
             drop-node-path) nil

            (or (isa? dragged-node-type
                      :heraldry/charge)
                (isa? dragged-node-type
                      :heraldry/charge-group)) #{:inside}

            (not= dragged-node-type
                  :heraldry/helm) nil

            (field-component/sibling?
             dragged-node-path
             drop-node-path) (let [current-index (last dragged-node-path)
                                   new-index (last drop-node-path)]
                               (cond
                                 (= new-index
                                    (dec current-index)) #{:above}

                                 (= new-index
                                    (inc current-index)) #{:below}

                                 :else #{:above :below}))
            :else nil)

    drop-node-open? (disj :below)))

(defn drop-fn
  [dragged-node-context drop-node-context where]
  (let [new-index (last (:path drop-node-context))
        target-context (case where
                         :above (-> drop-node-context c/-- (c/++ new-index))
                         :inside (c/++ drop-node-context :components component.element/APPEND-INDEX)
                         :below (-> drop-node-context c/-- (c/++ (inc new-index))))]
    (rf/dispatch [::component.element/move-general
                  dragged-node-context
                  target-context
                  {:no-select? true}])))

(defmethod component/node :heraldry/helm [{:keys [path] :as context}]
  (let [{:keys [helmet?
                torse?]} @(rf/subscribe [::status path])
        components-context (c/++ context :components)
        num-helms (interface/get-list-size (c/-- context))
        num-components (interface/get-list-size components-context)
        add-menu (cond-> []
                   (not helmet?) (conj {:title :string.entity/helmet
                                        :handler #(rf/dispatch [::component.element/add components-context default/helmet
                                                                shield-separator/add-element-options])})
                   (not torse?) (conj {:title :string.entity/torse
                                       :handler #(rf/dispatch [::component.element/add components-context default/torse
                                                               shield-separator/add-element-options])})
                   true (conj {:title :string.entity/crest-charge
                               :handler #(rf/dispatch [::component.element/add components-context default/crest-charge
                                                       shield-separator/add-element-options])})
                   true (conj {:title :string.entity/crest-charge-group
                               :handler #(rf/dispatch [::component.element/add components-context default/crest-charge-group
                                                       shield-separator/add-element-options])}))]

    {:title (string/str-tr (when (> num-helms 1)
                             (str (inc (last path)) ". ")) :string.entity/helm)
     :selectable? false
     :buttons (when (seq add-menu)
                [{:icon "fas fa-plus"
                  :title :string.button/add
                  :menu add-menu}])
     :draggable? true
     :drop-options-fn drop-options-fn
     :drop-fn drop-fn
     :nodes (->> (range num-components)
                 (map (fn [idx]
                        (let [component-context (c/++ components-context idx)
                              removable? @(rf/subscribe [::component.element/removable? component-context])]
                          {:context component-context
                           :buttons (cond-> [{:icon "fas fa-chevron-up"
                                              :disabled? (zero? idx)
                                              :title :string.tooltip/move-down
                                              :handler #(rf/dispatch [::component.element/move component-context (dec idx)])}
                                             {:icon "fas fa-chevron-down"
                                              :disabled? (= idx (dec num-components))
                                              :title :string.tooltip/move-up
                                              :handler #(rf/dispatch [::component.element/move component-context (inc idx)])}]
                                      removable? (conj
                                                  {:icon "far fa-trash-alt"
                                                   :remove? true
                                                   :title :string.tooltip/remove
                                                   :handler #(rf/dispatch [::component.element/remove component-context
                                                                           shield-separator/remove-element-options])}))})))
                 vec)}))
