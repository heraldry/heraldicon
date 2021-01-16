(ns heraldry.frontend.charge-library
  (:require ["svgo-browser/lib/get-svgo-instance" :as getSvgoInstance]
            [cljs-http.client :as http]
            [cljs.core.async :refer [go <!]]
            [cljs.core.async.interop :refer-macros [<p!]]
            [cljs.reader :as reader]
            [clojure.string :as s]
            [com.wsscode.common.async-cljs :refer [<? go-catch]]
            [heraldry.api.request :as api-request]
            [heraldry.coat-of-arms.filter :as filter]
            [heraldry.coat-of-arms.render :as render]
            [heraldry.coat-of-arms.tincture :as tincture]
            [heraldry.frontend.form.component :as component]
            [heraldry.frontend.form.core :as form]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.user :as user]
            [hickory.core :as hickory]
            [re-frame.core :as rf]))

(defn find-colours [data]
  (->> data
       (tree-seq #(or (map? %)
                      (vector? %)
                      (seq? %)) seq)
       (filter #(and (vector? %)
                     (-> % count (= 2))
                     (#{:fill :stroke} (first %))))
       (map second)
       (filter #(and (string? %)
                     (not= % "none")))
       (map s/lower-case)
       set))

;; functions


(defn fetch-charge-list-by-user [user-id]
  (go
    (let [db-path   [:charge-list]
          user-data (user/data)]
      (rf/dispatch-sync [:set db-path :loading])
      (-> (api-request/call :list-charges {:user-id user-id} user-data)
          <!
          (as-> response
              (let [error (:error response)]
                (if error
                  (println "fetch-charges-by-user error:" error)
                  (rf/dispatch-sync [:set db-path (:charges response)]))))))))

(defn fetch-url-data-to-path [db-path url function]
  (go
    (-> (http/get url)
        <!
        (as-> response
            (let [status (:status response)
                  body   (:body response)]
              (if (= status 200)
                (do
                  (println "retrieved" url)
                  (rf/dispatch [:set db-path (if function
                                               (function body)
                                               body)]))
                (println "error fetching" url)))))))

(defn fetch-charge-and-fill-form [charge-id]
  (go
    (let [form-db-path [:charge-form]
          user-data    (user/data)]
      (rf/dispatch-sync [:set form-db-path :loading])
      (-> (api-request/call :fetch-charge {:id charge-id} user-data)
          <!
          (as-> response
              (if-let [error (:error response)]
                (println ":fetch-charge-by-id error:" error)
                (do
                  (rf/dispatch [:set form-db-path response])
                  (fetch-url-data-to-path (conj form-db-path :data :edn-data)
                                          (:edn-data-url response) reader/read-string)
                  (fetch-url-data-to-path (conj form-db-path :data :svg-data)
                                          (:svg-data-url response) nil))))))))

(defn charge-path [charge-id]
  (str "/charges/#" charge-id))

(defn optimize-svg [data]
  (go-catch
   (-> {:removeUnknownsAndDefaults false}
       clj->js
       getSvgoInstance
       (.optimize data)
       <p!
       (js->clj :keywordize-keys true)
       :data)))

;; views


(defn load-svg-file [db-path data]
  (go
    (try
      (-> data
          optimize-svg
          <?
          hickory/parse-fragment
          first
          hickory/as-hiccup
          (as-> parsed
              (let [edn-data (-> parsed
                                 (assoc 0 :g)
                                 (assoc 1 {}))
                    width    (-> parsed
                                 (get-in [1 :width])
                                 js/parseFloat)
                    height   (-> parsed
                                 (get-in [1 :height])
                                 js/parseFloat)
                    colours  (into {}
                                   (map (fn [c]
                                          [c :keep]) (find-colours
                                                      edn-data)))]
                (rf/dispatch [:set (conj db-path :width) width])
                (rf/dispatch [:set (conj db-path :height) height])
                (rf/dispatch [:set (conj db-path :data) {:edn-data {:data    edn-data
                                                                    :width   width
                                                                    :height  height
                                                                    :colours colours}
                                                         :svg-data data}]))))
      (catch :default e
        (println "error:" e)))))

(defn preview [charge-data]
  (let [{:keys [edn-data]} charge-data
        render-options     @(rf/subscribe [:get [:render-options]])
        escutcheon         @(rf/subscribe [:get [:coat-of-arms :escutcheon]])
        example-coa        @(rf/subscribe [:get [:example-coa]])]
    [:svg {:viewBox             (str "0 0 520 700")
           :preserveAspectRatio "xMidYMid meet"
           :style               {:width "97%"}}
     [:defs
      filter/shadow
      filter/shiny
      filter/glow
      tincture/patterns]
     [:g {:transform "translate(10,10) scale(5,5)"}
      [render/coat-of-arms
       {:escutcheon escutcheon
        :field      (-> example-coa
                        (assoc-in [:components 0 :type] edn-data))}
       render-options
       :width 100]]]))

(defn upload-file [event db-path]
  (let [file (-> event .-target .-files (.item 0))]
    (when file
      (let [reader (js/FileReader.)]
        (set! (.-onloadend reader) (fn []
                                     (let [raw-data (.-result reader)]
                                       (load-svg-file db-path raw-data))))
        (.readAsText reader file)))))

(defn save-charge-clicked [db-path]
  (let [payload   @(rf/subscribe [:get db-path])
        user-data (user/data)]
    (go
      (try
        (let [response  (<! (api-request/call :save-charge payload user-data))
              error     (:error response)
              charge-id (-> response :charge-id)]
          (println "save charge response" response)
          (when-not error
            (rf/dispatch [:set (conj db-path :id) charge-id])
            (state/goto (charge-path charge-id))))
        (catch :default e
          (println "save-form error:" e))))))

(defn charge-form []
  (let [db-path        [:charge-form]
        error-message  @(rf/subscribe [:get-form-error db-path])
        {:keys [data]} @(rf/subscribe [:get db-path])
        on-submit      (fn [event]
                         (.preventDefault event)
                         (.stopPropagation event)
                         (save-charge-clicked db-path))]
    [:div.pure-g
     [:div.pure-u-1-2 {:style {:position "fixed"}}
      [preview data]]
     [:div.pure-u-1-2 {:style {:margin-left "50%"}}
      [:form.pure-form.pure-form-aligned
       {:style        {:display "inline-block"}
        :on-key-press (fn [event]
                        (when (-> event .-code (= "Enter"))
                          (on-submit event)))
        :on-submit    on-submit}
       (when error-message
         [:div.error-message error-message])
       [:fieldset
        [form/field (conj db-path :name)
         (fn [& {:keys [value on-change]}]
           [:div.pure-control-group
            [:label {:for   "name"
                     :style {:width "6em"}} "Name"]
            [:input {:id        "name"
                     :value     value
                     :on-change on-change
                     :type      "text"
                     :style     {:margin-right "0.5em"}}]
            [form/checkbox (conj db-path :is-public) "Make public"
             :style {:width "7em"}]])]
        [form/field (conj db-path :key)
         (fn [& {:keys [value on-change]}]
           [:div.pure-control-group
            [:label {:for   "key"
                     :style {:width "6em"}} "Charge Key"]
            [:input {:id        "key"
                     :value     value
                     :on-change on-change
                     :type      "text"}]])]
        [form/select (conj db-path :attitude) "Attitude" [["None" :none]
                                                          ["Couchant" :couchant]
                                                          ["Courant" :courant]
                                                          ["Dormant" :dormant]
                                                          ["Pascuant" :pascuant]
                                                          ["Passant" :passant]
                                                          ["Rampant" :rampant]
                                                          ["Salient" :salient]
                                                          ["Sejant" :sejant]
                                                          ["Statant" :statant]]
         :label-style {:width "6em"}]
        [form/select (conj db-path :facing) "Facing" [["None" :none]
                                                      ["To dexter" :to-dexter]
                                                      ["To sinister" :to-sinister]
                                                      ["Affronté" :affronte]
                                                      ["En arrière" :en-arriere]
                                                      ["Guardant" :guardant]
                                                      ["Reguardant" :reguardant]
                                                      ["Salient" :salient]
                                                      ["In trian aspect" :in-trian-aspect]]
         :label-style {:width "6em"}]
        [:div.pure-control-group
         [:h4 {:style {:margin-top    "1em"
                       :margin-bottom "0.5em"}} "Attributes"]
         [form/checkbox (conj db-path :attributes :coward) "Coward"]
         [form/checkbox (conj db-path :attributes :pierced) "Pierced"]
         [form/checkbox (conj db-path :attributes :voided) "Voided"]]
        [:div.pure-control-group
         [:h4 {:style {:margin-top    "1em"
                       :margin-bottom "0.5em"}} "Colours"]
         (let [colours-path   (conj db-path :data :edn-data :colours)
               colours        @(rf/subscribe [:get colours-path])
               colour-options [["Keep" :keep]
                               ["Primary" :primary]
                               ["Outline" :outline]
                               ["Eyes and Teeth" :eyes-and-teeth]
                               ["Armed" :armed]
                               ["Langued" :langued]
                               ["Attired" :attired]
                               ["Unguled" :unguled]
                               ["Beaked" :beaked]]]
           (for [[k _] colours]
             ^{:key k}
             [form/select (conj colours-path k)
              [:div.colour-preview.tooltip {:style {:background-color k}}
               [:div.bottom {:style {:top "30px"}}
                [:h3 {:style {:text-align "center"}} k]
                [:i]]]
              colour-options
              :label-style {:width        "1.5em"
                            :margin-right "0.5em"}
              :style {:display      "inline-block"
                      :padding-left "0"
                      :margin-right "0.5em"}]))]

        [form/field (conj db-path :data)
         (fn [& _]
           [:div.pure-control-group {:style {:margin-top "3em"}}
            [:label {:for "upload"} "Upload"]
            [:input {:type      "file"
                     :accept    "image/svg+xml"
                     :id        "upload"
                     :on-change #(upload-file % db-path)}]])]]
       [:div.pure-control-group {:style {:text-align "right"
                                         :margin-top "10px"}}
        [:button.pure-button.pure-button-primary {:type "submit"}
         "Save"]]]
      [component/form-for-field [:example-coa]]
      [component/form-render-options]]]))

(defn list-charges-for-user []
  (let [user-data   (user/data)
        charge-list @(rf/subscribe [:get [:charge-list]])]
    [:div {:style {:padding "15px"}}
     [:h4 "My charges"]
     [:button.pure-button.pure-button-primary {:on-click #(rf/dispatch [:set [:charge-form] {}])}
      "Create"]
     (cond
       (nil? charge-list)       (do
                                  (fetch-charge-list-by-user (:user-id user-data))
                                  [:<>])
       (= charge-list :loading) [:<>]
       :else                    [:ul.charge-list
                                 (doall
                                  (for [charge charge-list]
                                    ^{:key (:id charge)}
                                    [:li.charge
                                     (let [href (str (state/path) "#" (:id charge))]
                                       [:a {:href     href
                                            :on-click #(do
                                                         (.preventDefault %)
                                                         (state/goto href))}
                                        (:name charge) " "
                                        [:i.far.fa-edit]])]))])]))

(defn logged-in []
  (let [charge-form-data @(rf/subscribe [:get [:charge-form]])
        path-extra       (state/path-extra)]
    (cond
      (and path-extra
           (nil? charge-form-data)) (do
                                      (fetch-charge-and-fill-form path-extra)
                                      [:<>])
      (= charge-form-data :loading) [:<>]
      charge-form-data              [charge-form]
      :else                         [list-charges-for-user])))

(defn not-logged-in []
  [:div {:style {:padding "15px"}}
   "You need to be logged in."])

(defn main []
  (let [user-data (user/data)]
    (if (:logged-in? user-data)
      [logged-in]
      [not-logged-in])))
