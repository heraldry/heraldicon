(ns heraldry.frontend.charge-library
  (:require ["svgo-browser/lib/get-svgo-instance" :as getSvgoInstance]
            [cljs.core.async :refer [go]]
            [cljs.core.async.interop :refer-macros [<p!]]
            [clojure.string :as s]
            [com.wsscode.common.async-cljs :refer [<? go-catch]]
            [heraldry.api.request :as api-request]
            [heraldry.coat-of-arms.render :as render]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.frontend.context :as context]
            [heraldry.frontend.form.component :as component]
            [heraldry.frontend.form.core :as form]
            [heraldry.frontend.http :as http]
            [heraldry.frontend.user :as user]
            [heraldry.frontend.util :as util]
            [hickory.core :as hickory]
            [re-frame.core :as rf]
            [reitit.frontend.easy :as reife]))

(def form-db-path
  [:charge-form])

(def list-db-path
  [:charge-list])

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
       (map svg/normalize-colour)
       (map s/lower-case)
       set))

(defn fetch-charge-list-by-user [user-id]
  (go
    (try
      (let [user-data (user/data)]
        (rf/dispatch-sync [:set list-db-path :loading])
        (rf/dispatch-sync [:set list-db-path (-> (api-request/call :list-charges {:user-id user-id} user-data)
                                                 <?
                                                 :charges)]))
      (catch :default e
        (println "fetch-charges-by-user error:" e)))))

(defn fetch-charge-and-fill-form [charge-id]
  (go
    (try
      (rf/dispatch-sync [:set form-db-path :loading])
      (let [user-data (user/data)
            response  (<? (api-request/call :fetch-charge {:id charge-id} user-data))
            edn-data  (<? (http/fetch (:edn-data-url response)))
            svg-data  (<? (http/fetch (:svg-data-url response)))]
        (rf/dispatch-sync [:set form-db-path (-> response
                                                 (assoc-in [:data :edn-data] edn-data)
                                                 (assoc-in [:data :svg-data] svg-data))]))
      (catch :default e
        (println ":fetch-charge-by-id error:" e)))))

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
  (go-catch
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

(defn preview []
  (let [{:keys [data type]}    @(rf/subscribe [:get form-db-path])
        {:keys [edn-data]}     data
        db-path                [:example-coa :coat-of-arms]
        render-options         @(rf/subscribe [:get [:example-coa :render-options]])
        coat-of-arms           @(rf/subscribe [:get db-path])
        {:keys [result
                environment]}  (render/coat-of-arms
                                (-> coat-of-arms
                                    (assoc-in [:field :components 0 :type] type)
                                    (assoc-in [:field :components 0 :data] edn-data))
                                100
                                (merge
                                 context/default
                                 {:render-options render-options
                                  :db-path        db-path}))
        {:keys [width height]} environment]
    [:svg {:viewBox             (str "0 0 " (-> width (* 5) (+ 20)) " " (-> height (* 5) (+ 20) (+ 30)))
           :preserveAspectRatio "xMidYMid meet"
           :style               {:width "97%"}}
     [:g {:transform "translate(10,10) scale(5,5)"}
      result]]))

(defn upload-file [event db-path]
  (let [file (-> event .-target .-files (.item 0))]
    (when file
      (let [reader (js/FileReader.)]
        (set! (.-onloadend reader) (fn []
                                     (let [raw-data (.-result reader)]
                                       (load-svg-file db-path raw-data))))
        (.readAsText reader file)))))

(defn save-charge-clicked []
  (let [payload   @(rf/subscribe [:get form-db-path])
        user-data (user/data)]
    (rf/dispatch-sync [:clear-form-errors form-db-path])
    (go
      (try
        (let [response  (<? (api-request/call :save-charge payload user-data))
              charge-id (-> response :charge-id)]
          (println "save charge response" response)
          (rf/dispatch-sync [:set (conj form-db-path :id) charge-id])
          (reife/push-state :charge-by-id {:id (util/id-for-url charge-id)}))
        (catch :default e
          (println "save-form error:" e)
          (rf/dispatch [:set-form-error form-db-path (:message (ex-data e))]))))))

(defn charge-form []
  (let [error-message @(rf/subscribe [:get-form-error form-db-path])
        on-submit     (fn [event]
                        (.preventDefault event)
                        (.stopPropagation event)
                        (save-charge-clicked))]
    [:div.pure-g {:on-click #(do (rf/dispatch [:ui-component-deselect-all])
                                 (rf/dispatch [:ui-submenu-close-all])
                                 (.stopPropagation %))}
     [:div.pure-u-1-2 {:style {:position "fixed"}}
      [preview]]
     [:div.pure-u-1-2 {:style {:margin-left "50%"
                               :width       "45%"}}
      [component/form-attribution (conj form-db-path :attribution)]
      [:form.pure-form.pure-form-aligned
       {:style        {:display "inline-block"}
        :on-key-press (fn [event]
                        (when (-> event .-code (= "Enter"))
                          (on-submit event)))
        :on-submit    on-submit}
       (when error-message
         [:div.error-message error-message])
       [:fieldset
        [form/field (conj form-db-path :name)
         (fn [& {:keys [value on-change]}]
           [:div.pure-control-group
            [:label {:for   "name"
                     :style {:width "6em"}} "Name"]
            [:input {:id        "name"
                     :value     value
                     :on-change on-change
                     :type      "text"
                     :style     {:margin-right "0.5em"}}]
            [form/checkbox (conj form-db-path :is-public) "Make public"
             :style {:width "7em"}]])]
        [form/field (conj form-db-path :type)
         (fn [& {:keys [value on-change]}]
           [:div.pure-control-group
            [:label {:for   "type"
                     :style {:width "6em"}} "Charge type"]
            [:input {:id        "type"
                     :value     value
                     :on-change on-change
                     :type      "text"}]])]
        [form/select (conj form-db-path :attitude) "Attitude" [["None" :none]
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
        [form/select (conj form-db-path :facing) "Facing" [["None" :none]
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
         [form/checkbox (conj form-db-path :attributes :coward) "Coward"]
         [form/checkbox (conj form-db-path :attributes :pierced) "Pierced"]
         [form/checkbox (conj form-db-path :attributes :voided) "Voided"]]
        [:div.pure-control-group
         [:h4 {:style {:margin-top    "1em"
                       :margin-bottom "0.5em"}} "Colours"]
         (let [colours-path   (conj form-db-path :data :edn-data :colours)
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
           (for [[k _] (sort-by first colours)]
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
                      :margin-right "0.5em"}]))
         [form/checkbox [:example-coa :render-options :preview-original?]
          "Preview original (don't replace colours)" :style {:margin-right "0.5em"
                                                             :width        "20em"}]]]
       [:div.pure-control-group {:style {:text-align "right"
                                         :margin-top "10px"}}
        [form/field-without-error (conj form-db-path :data)
         (fn [& _]
           [:label.pure-button {:for   "upload"
                                :style {:display "inline-block"
                                        :width   "auto"
                                        :float   "left"}} "Upload SVG"
            [:input {:type      "file"
                     :accept    "image/svg+xml"
                     :id        "upload"
                     :on-change #(upload-file % form-db-path)
                     :style     {:display "none"}}]])]
        [:button.pure-button.pure-button-primary {:type "submit"}
         "Save"]]]
      [component/form-render-options [:example-coa :render-options]]
      [component/form-for-coat-of-arms [:example-coa :coat-of-arms]]]]))

(defn list-charges-for-user []
  (let [user-data   (user/data)
        charge-list @(rf/subscribe [:get list-db-path])]
    [:div {:style {:padding "15px"}}
     [:h4 "My charges"]
     [:button.pure-button.pure-button-primary
      {:on-click #(do
                    (rf/dispatch-sync [:set form-db-path nil])
                    (reife/push-state :create-charge))}
      "Create"]
     (cond
       (nil? charge-list)       (do
                                  (fetch-charge-list-by-user (:user-id user-data))
                                  [:<>])
       (= charge-list :loading) [:<>]
       :else                    [:ul.charge-list
                                 (doall
                                  (for [charge charge-list]
                                    (let [charge-id (-> charge
                                                        :id
                                                        util/id-for-url)]
                                      ^{:key charge-id}
                                      [:li.charge
                                       [:a {:href     (reife/href :charge-by-id {:id charge-id})
                                            :on-click #(do
                                                         (rf/dispatch-sync [:set form-db-path nil])
                                                         (reife/href :charge-by-id {:id charge-id}))}
                                        (:name charge) " "
                                        [:i.far.fa-edit]]])))])]))

(defn create-charge []
  [charge-form])

(defn charge-by-id [charge-id]
  (let [charge-form-data @(rf/subscribe [:get form-db-path])]
    (cond
      (and charge-id
           (nil? charge-form-data)) (do
                                      (fetch-charge-and-fill-form charge-id)
                                      [:<>])
      (= charge-form-data :loading) [:<>]
      charge-form-data              [charge-form]
      :else                         [:<>])))

(defn not-logged-in []
  [:div {:style {:padding "15px"}}
   "You need to be logged in."])

(defn view-list-charges []
  (let [user-data (user/data)]
    (if (:logged-in? user-data)
      [list-charges-for-user]
      [not-logged-in])))

(defn view-charge-by-id [{:keys [parameters]}]
  (let [user-data (user/data)
        charge-id (str "charge:" (-> parameters :path :id))]
    (if (:logged-in? user-data)
      [charge-by-id charge-id]
      [not-logged-in])))
