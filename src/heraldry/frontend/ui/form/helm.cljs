(ns heraldry.frontend.ui.form.helm
  (:require [heraldry.coat-of-arms.default :as default]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.ui.element.submenu :as submenu]
            [heraldry.frontend.ui.interface :as ui-interface]
            [heraldry.macros :as macros]
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

(defn form [path _]
  [:<>
   (for [option []]
     ^{:key option} [ui-interface/form-element (conj path option)])])

(defmethod ui-interface/component-node-data :heraldry.component/helm [path]
  (let [{:keys [helmet?
                torse?]} @(rf/subscribe [:get-helm-status path])
        components-path (conj path :components)
        num-helms @(rf/subscribe [:get-list-size (-> path drop-last vec)])
        num-components @(rf/subscribe [:get-list-size components-path])
        add-menu (cond-> []
                   (not helmet?) (conj {:title "Helmet"
                                        :handler #(state/dispatch-on-event
                                                   % [:add-element components-path default/helmet])})
                   (not torse?) (conj {:title "Torse"
                                       :handler #(state/dispatch-on-event
                                                  % [:add-element components-path default/torse])})
                   true (conj {:title "Crest charge"
                               :handler #(state/dispatch-on-event
                                          % [:add-element components-path default/crest-charge])}))]

    {:title (str (when (> num-helms 1)
                   (str (inc (last path)) ". ")) "Helm")
     :buttons (when (seq add-menu)
                [{:icon "fas fa-plus"
                  :title "Add"
                  :menu add-menu}])
     :nodes (->> (range num-components)
                 reverse
                 (map (fn [idx]
                        (let [component-path (conj components-path idx)]
                          {:path component-path
                           :buttons [{:icon "fas fa-chevron-down"
                                      :disabled? (zero? idx)
                                      :tooltip "move down"
                                      :handler #(state/dispatch-on-event % [:move-element component-path (dec idx)])}
                                     {:icon "fas fa-chevron-up"
                                      :disabled? (= idx (dec num-components))
                                      :tooltip "move up"
                                      :handler #(state/dispatch-on-event % [:move-element component-path (inc idx)])}
                                     {:icon "far fa-trash-alt"
                                      :tooltip "remove"
                                      :handler #(state/dispatch-on-event % [:remove-element component-path])}]})))
                 vec)}))

(defmethod ui-interface/component-form-data :heraldry.component/helm [_path]
  {:form form})
