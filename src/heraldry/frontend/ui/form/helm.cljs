(ns heraldry.frontend.ui.form.helm
  (:require
   [heraldry.coat-of-arms.default :as default]
   [heraldry.context :as c]
   [heraldry.frontend.state :as state]
   [heraldry.frontend.ui.interface :as ui-interface]
   [heraldry.interface :as interface]
   [heraldry.shield-separator :as shield-separator]
   [heraldry.util :as util]
   [re-frame.core :as rf]))

(rf/reg-sub :get-helm-status
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

(defn form [_context]
  [:<>])

(defmethod ui-interface/component-node-data :heraldry.component/helm [{:keys [path] :as context}]
  (let [{:keys [helmet?
                torse?]} @(rf/subscribe [:get-helm-status path])
        components-context (c/++ context :components)
        num-helms (interface/get-list-size (c/-- context))
        num-components (interface/get-list-size components-context)
        add-menu (cond-> []
                   (not helmet?) (conj {:title :string.entity/helmet
                                        :handler #(state/dispatch-on-event
                                                   % [:add-element components-context default/helmet
                                                      shield-separator/add-element-options])})
                   (not torse?) (conj {:title :string.entity/torse
                                       :handler #(state/dispatch-on-event
                                                  % [:add-element components-context default/torse
                                                     shield-separator/add-element-options])})
                   true (conj {:title :string.entity/crest-charge
                               :handler #(state/dispatch-on-event
                                          % [:add-element components-context default/crest-charge
                                             shield-separator/add-element-options])}))]

    {:title (util/str-tr (when (> num-helms 1)
                           (str (inc (last path)) ". ")) :string.entity/helm)
     :buttons (when (seq add-menu)
                [{:icon "fas fa-plus"
                  :title :string.button/add
                  :menu add-menu}])
     :nodes (->> (range num-components)
                 reverse
                 (map (fn [idx]
                        (let [component-context (c/++ components-context idx)
                              removable? @(rf/subscribe [:element-removable? component-context])]
                          {:context component-context
                           :buttons (cond-> [{:icon "fas fa-chevron-down"
                                              :disabled? (zero? idx)
                                              :tooltip :string.tooltip/move-down
                                              :handler #(state/dispatch-on-event % [:move-element component-context (dec idx)])}
                                             {:icon "fas fa-chevron-up"
                                              :disabled? (= idx (dec num-components))
                                              :tooltip :string.tooltip/move-up
                                              :handler #(state/dispatch-on-event % [:move-element component-context (inc idx)])}]
                                      removable? (conj
                                                  {:icon "far fa-trash-alt"
                                                   :remove? true
                                                   :tooltip :string.tooltip/remove
                                                   :handler #(state/dispatch-on-event
                                                              %
                                                              [:remove-element component-context
                                                               shield-separator/remove-element-options])}))})))
                 vec)}))

(defmethod ui-interface/component-form-data :heraldry.component/helm [_context]
  {:form form})
