(ns heraldicon.frontend.component.helm
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.component.core :as component]
   [heraldicon.frontend.component.element :as element]
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

(defmethod component/node :heraldry/helm [{:keys [path] :as context}]
  (let [{:keys [helmet?
                torse?]} @(rf/subscribe [::status path])
        components-context (c/++ context :components)
        num-helms (interface/get-list-size (c/-- context))
        num-components (interface/get-list-size components-context)
        add-menu (cond-> []
                   (not helmet?) (conj {:title :string.entity/helmet
                                        :handler #(rf/dispatch [::element/add components-context default/helmet
                                                                shield-separator/add-element-options])})
                   (not torse?) (conj {:title :string.entity/torse
                                       :handler #(rf/dispatch [::element/add components-context default/torse
                                                               shield-separator/add-element-options])})
                   true (conj {:title :string.entity/crest-charge
                               :handler #(rf/dispatch [::element/add components-context default/crest-charge
                                                       shield-separator/add-element-options])})
                   true (conj {:title :string.entity/crest-charge-group
                               :handler #(rf/dispatch [::element/add components-context default/crest-charge-group
                                                       shield-separator/add-element-options])}))]

    {:title (string/str-tr (when (> num-helms 1)
                             (str (inc (last path)) ". ")) :string.entity/helm)
     :selectable? false
     :buttons (when (seq add-menu)
                [{:icon "fas fa-plus"
                  :title :string.button/add
                  :menu add-menu}])
     :nodes (->> (range num-components)
                 reverse
                 (map (fn [idx]
                        (let [component-context (c/++ components-context idx)
                              removable? @(rf/subscribe [::element/removable? component-context])]
                          {:context component-context
                           :buttons (cond-> [{:icon "fas fa-chevron-down"
                                              :disabled? (zero? idx)
                                              :title :string.tooltip/move-down
                                              :handler #(rf/dispatch [::element/move component-context (dec idx)])}
                                             {:icon "fas fa-chevron-up"
                                              :disabled? (= idx (dec num-components))
                                              :title :string.tooltip/move-up
                                              :handler #(rf/dispatch [::element/move component-context (inc idx)])}]
                                      removable? (conj
                                                  {:icon "far fa-trash-alt"
                                                   :remove? true
                                                   :title :string.tooltip/remove
                                                   :handler #(rf/dispatch [::element/remove component-context
                                                                           shield-separator/remove-element-options])}))})))
                 vec)}))
