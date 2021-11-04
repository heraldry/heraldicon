(ns heraldry.frontend.ui.form.helm
  (:require
   [heraldry.coat-of-arms.default :as default]
   [heraldry.context :as c]
   [heraldry.frontend.macros :as macros]
   [heraldry.frontend.state :as state]
   [heraldry.frontend.ui.element.submenu :as submenu]
   [heraldry.frontend.ui.interface :as ui-interface]
   [heraldry.interface :as interface]
   [heraldry.shield-separator :as shield-separator]
   [heraldry.strings :as strings]
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

(macros/reg-event-db :add-helm-part
  (fn [db [_ path value]]
    (-> db
        (assoc-in path value)
        (state/ui-component-node-select path :open? true)
        submenu/ui-submenu-close-all)))

(defn form [_context]
  [:<>])

(defmethod ui-interface/component-node-data :heraldry.component/helm [{:keys [path] :as context}]
  (let [{:keys [helmet?
                torse?]} @(rf/subscribe [:get-helm-status path])
        components-path (conj path :components)
        num-helms (interface/get-list-size (c/<< context :path (-> path drop-last vec)))
        num-components (interface/get-list-size (c/++ context :components))
        add-menu (cond-> []
                   (not helmet?) (conj {:title {:en "Helmet"
                                                :de "Helm"}
                                        :handler #(state/dispatch-on-event
                                                   % [:add-element components-path default/helmet
                                                      shield-separator/add-element-options])})
                   (not torse?) (conj {:title {:en "Torse"
                                               :de "Helmwulst"}
                                       :handler #(state/dispatch-on-event
                                                  % [:add-element components-path default/torse
                                                     shield-separator/add-element-options])})
                   true (conj {:title {:en "Crest charge"
                                       :de "Helmzier Figur"}
                               :handler #(state/dispatch-on-event
                                          % [:add-element components-path default/crest-charge
                                             shield-separator/add-element-options])}))]

    {:title (util/str-tr (when (> num-helms 1)
                           (str (inc (last path)) ". ")) {:en "Helm"
                                                          :de "Helm"})
     :buttons (when (seq add-menu)
                [{:icon "fas fa-plus"
                  :title strings/add
                  :menu add-menu}])
     :nodes (->> (range num-components)
                 reverse
                 (map (fn [idx]
                        (let [component-path (conj components-path idx)
                              removable? @(rf/subscribe [:element-removable? component-path])]
                          {:context (c/<< context :path component-path)
                           :buttons (cond-> [{:icon "fas fa-chevron-down"
                                              :disabled? (zero? idx)
                                              :tooltip strings/move-down
                                              :handler #(state/dispatch-on-event % [:move-element component-path (dec idx)])}
                                             {:icon "fas fa-chevron-up"
                                              :disabled? (= idx (dec num-components))
                                              :tooltip strings/move-up
                                              :handler #(state/dispatch-on-event % [:move-element component-path (inc idx)])}]
                                      removable? (conj
                                                  {:icon "far fa-trash-alt"
                                                   :tooltip strings/remove
                                                   :handler #(state/dispatch-on-event
                                                              %
                                                              [:remove-element component-path
                                                               shield-separator/remove-element-options])}))})))
                 vec)}))

(defmethod ui-interface/component-form-data :heraldry.component/helm [_context]
  {:form form})
