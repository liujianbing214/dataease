<template>
  <div
    v-loading="canvasStyleData.refreshViewLoading&&requestStatus==='waiting'"
    :class="[
      {
        ['active']: active
      },
      'rect-shape'
    ]"
  >
    <!--    <i v-if="requestStatus==='success'" style="right:25px;position: absolute;z-index: 2" class="icon iconfont icon-fangda" @click.stop="openChartDetailsDialog" />-->
    <div v-if="requestStatus==='error'" class="chart-error-class">
      <div style="font-size: 12px; color: #9ea6b2;height: 100%;display: flex;align-items: center;justify-content: center;">
        {{ message }},{{ $t('chart.chart_show_error') }}
        <br>
        {{ $t('chart.chart_error_tips') }}
      </div>
    </div>
    <chart-component v-if="httpRequest.status &&chart.type && !chart.type.includes('table') && !chart.type.includes('text') && renderComponent() === 'echarts'" :ref="element.propValue.id" class="chart-class" :chart="chart" :track-menu="trackMenu" @onChartClick="chartClick" @onJumpClick="jumpClick" />
    <chart-component-g2 v-if="httpRequest.status &&chart.type && !chart.type.includes('table') && !chart.type.includes('text') && renderComponent() === 'antv'" :ref="element.propValue.id" class="chart-class" :chart="chart" :track-menu="trackMenu" @onChartClick="chartClick" @onJumpClick="jumpClick" />
    <!--    <chart-component :ref="element.propValue.id" class="chart-class" :chart="chart" :track-menu="trackMenu" @onChartClick="chartClick" />-->
    <table-normal v-if="httpRequest.status &&chart.type && chart.type.includes('table')" :ref="element.propValue.id" :show-summary="chart.type === 'table-normal'" :chart="chart" class="table-class" />
    <label-normal v-if="httpRequest.status && chart.type && chart.type.includes('text')" :ref="element.propValue.id" :chart="chart" class="table-class" />
    <div style="position: absolute;left: 20px;bottom:14px;">
      <drill-path :drill-filters="drillFilters" @onDrillJump="drillJump" />
    </div>
  </div>
</template>

<script>

import { viewData } from '@/api/panel/panel'
import { viewInfo } from '@/api/link'
import ChartComponent from '@/views/chart/components/ChartComponent.vue'
import TableNormal from '@/views/chart/components/table/TableNormal'
import LabelNormal from '../../../views/chart/components/normal/LabelNormal'
import { uuid } from 'vue-uuid'

import { mapState } from 'vuex'
import { isChange } from '@/utils/conditionUtil'
import { BASE_CHART_STRING } from '@/views/chart/chart/chart'
import eventBus from '@/components/canvas/utils/eventBus'
import { deepCopy } from '@/components/canvas/utils/utils'
import { getToken, getLinkToken } from '@/utils/auth'
import DrillPath from '@/views/chart/view/DrillPath'
import { areaMapping } from '@/api/map/map'
import ChartComponentG2 from '@/views/chart/components/ChartComponentG2'
import { Base64 } from 'js-base64'
export default {
  name: 'UserView',
  components: { ChartComponent, TableNormal, LabelNormal, DrillPath, ChartComponentG2 },
  props: {
    element: {
      type: Object,
      default: null
    },
    // filters: {
    //   type: Array,
    //   required: false,
    //   default: null
    // },
    outStyle: {
      type: Object,
      required: false,
      default: function() {
        return {}
      }
    },
    searchCount: {
      type: Number,
      required: false,
      default: 0
    },
    active: {
      type: Boolean,
      required: false,
      default: false
    },
    componentIndex: {
      type: Number,
      required: false
    }
  },
  data() {
    return {
      refId: null,
      chart: BASE_CHART_STRING,
      requestStatus: 'waiting',
      message: null,
      drillClickDimensionList: [],
      drillFilters: [],
      drillFields: [],
      places: [],
      httpRequest: {
        status: true,
        msg: ''
      },
      timeMachine: null,
      changeIndex: 0
    }
  },
  computed: {
    panelInfo() {
      return this.$store.state.panel.panelInfo
    },
    filter() {
      const filter = {}
      filter.filter = this.element.filters
      filter.linkageFilters = this.element.linkageFilters
      filter.drill = this.drillClickDimensionList
      return filter
    },
    filters() {
      // 必要 勿删勿该  watch数组，哪怕发生变化 oldValue等于newValue ，深拷贝解决
      if (!this.element.filters) return []
      return JSON.parse(JSON.stringify(this.element.filters))
    },

    linkageFilters() {
      // 必要 勿删勿该  watch数组，哪怕发生变化 oldValue等于newValue ，深拷贝解决
      if (!this.element.linkageFilters) return []
      // console.log('linkageFilters:' + JSON.stringify(this.element.linkageFilters))
      return JSON.parse(JSON.stringify(this.element.linkageFilters))
    },
    trackMenu() {
      const trackMenuInfo = []
      let linkageCount = 0
      let jumpCount = 0
      this.chart.data && this.chart.data.sourceFields && this.chart.data.sourceFields.forEach(item => {
        const sourceInfo = this.chart.id + '#' + item.id
        if (this.nowPanelTrackInfo[sourceInfo]) {
          linkageCount++
        }
      })
      this.chart.data && this.chart.data.sourceFields && this.chart.data.sourceFields.forEach(item => {
        const sourceInfo = this.chart.id + '#' + item.id
        // console.log('nowPanelJumpInfo=>' + JSON.stringify(this.nowPanelJumpInfo))
        if (this.nowPanelJumpInfo[sourceInfo]) {
          jumpCount++
        }
      })
      jumpCount && trackMenuInfo.push('jump')
      linkageCount && trackMenuInfo.push('linkage')
      this.drillFields.length && trackMenuInfo.push('drill')
      // console.log('trackMenuInfo' + JSON.stringify(trackMenuInfo))
      return trackMenuInfo
    },
    chartType() {
      return this.chart.type
    },
    hw() {
      return this.outStyle.width * this.outStyle.height
    },
    ...mapState([
      'canvasStyleData',
      'nowPanelTrackInfo',
      'nowPanelJumpInfo'
    ])
  },

  watch: {
    'filters': function(val1, val2) {
      // this.getData(this.element.propValue.viewId)
      isChange(val1, val2) && this.getData(this.element.propValue.viewId)
    },
    linkageFilters: {
      handler(newVal, oldVal) {
        // isChange(newVal, oldVal) && this.getData(this.element.propValue.viewId)
        if (isChange(newVal, oldVal)) {
        //   if (this.chart.type === 'map') {
        //     this.doMapLink(newVal)
        //   }
          this.getData(this.element.propValue.viewId)
        }
      },
      deep: true
    },
    // deep监听panel 如果改变 提交到 store
    canvasStyleData: {
      handler(newVal, oldVla) {
        // this.chart.stylePriority == panel 优先使用仪表板样式
        this.mergeStyle()
      },
      deep: true
    },
    // 监听外部的样式变化 （非实时性要求）
    'hw': {
      handler(newVal, oldVla) {
        // console.log('hw:' + newVal + '---' + oldVla)
        if (newVal !== oldVla && this.$refs[this.element.propValue.id]) {
          if (this.chart.type === 'map') {
            this.destroyTimeMachine()
            this.changeIndex++
            this.chartResize(this.changeIndex)
          } else {
            this.$refs[this.element.propValue.id].chartResize()
          }
        }
      },
      deep: true
    },
    // 监听外部的样式变化 （非实时性要求）
    outStyle: {
      handler(newVal, oldVla) {
        //
        // if (this.$refs[this.element.propValue.id]) {
        //   this.$refs[this.element.propValue.id].chartResize()
        // }
      },
      deep: true
    },
    // 监听外部计时器变化
    searchCount: function(val1) {
      if (val1 > 0 && this.requestStatus !== 'waiting') {
        this.getData(this.element.propValue.viewId)
      }
    },
    'chartType': function(newVal, oldVal) {
      if (newVal === 'map' && newVal !== oldVal) {
        this.initAreas()
      }
    }
  },

  created() {
    this.refId = uuid.v1
    // this.filter.filter = this.$store.getters.conditions
    if (this.element && this.element.propValue && this.element.propValue.viewId) {
      this.getData(this.element.propValue.viewId)
    }

    // this.initAreas()
  },
  mounted() {
  },
  methods: {
    mergeStyle() {
      // this.chart.stylePriority == panel 优先使用仪表板样式
      if ((this.requestStatus === 'success' || this.requestStatus === 'merging') && this.chart.stylePriority === 'panel' && this.canvasStyleData.chart) {
        const customAttrChart = JSON.parse(this.chart.customAttr)
        const customStyleChart = JSON.parse(this.chart.customStyle)

        const customAttrPanel = JSON.parse(this.canvasStyleData.chart.customAttr)
        const customStylePanel = JSON.parse(this.canvasStyleData.chart.customStyle)

        // 组件样式-标题设置 - 标题修改为组件自己控制
        // customStyleChart.text = customStylePanel.text
        // 组件样式-背景设置
        customStyleChart.background = customStylePanel.background
        // 图形属性-颜色设置
        if (this.chart.type.includes('table')) {
          customAttrChart.color = customAttrPanel.tableColor
        } else {
          customAttrChart.color = customAttrPanel.color
        }

        this.chart = {
          ...this.chart,
          customAttr: JSON.stringify(customAttrChart),
          customStyle: JSON.stringify(customStyleChart)
        }
      }
    },
    getData(id) {
      if (id) {
        this.requestStatus = 'waiting'
        this.message = null

        // 增加判断 仪表板公共连接中使用viewInfo 正常使用viewData
        let method = viewData
        const token = this.$store.getters.token || getToken()
        const linkToken = this.$store.getters.linkToken || getLinkToken()
        if (!token && linkToken) {
          method = viewInfo
        }

        method(id, this.filter).then(response => {
          // 将视图传入echart组件
          if (response.success) {
            this.chart = response.data
            this.chart.drillFields = this.chart.drillFields ? JSON.parse(this.chart.drillFields) : []
            if (!response.data.drill) {
              this.drillClickDimensionList.splice(this.drillClickDimensionList.length - 1, 1)
              this.resetDrill()
            }
            this.drillFilters = JSON.parse(JSON.stringify(response.data.drillFilters ? response.data.drillFilters : []))
            this.drillFields = JSON.parse(JSON.stringify(response.data.drillFields))
            this.requestStatus = 'merging'
            this.mergeStyle()
            this.requestStatus = 'success'
            this.httpRequest.status = true
          } else {
            this.requestStatus = 'error'
            this.message = response.message
          }
          return true
        }).catch(err => {
          this.httpRequest.status = err.response.data.success
          this.httpRequest.msg = err.response.data.message
          this.requestStatus = 'error'
          if (err && err.response && err.response.data) {
            this.message = err.response.data.message
          } else {
            this.message = err
          }
          return true
        })
      }
    },
    viewIdMatch(viewIds, viewId) {
      return !viewIds || viewIds.length === 0 || viewIds.includes(viewId)
    },
    openChartDetailsDialog() {
      const tableChart = deepCopy(this.chart)
      tableChart.customAttr = JSON.parse(this.chart.customAttr)
      tableChart.customStyle = JSON.parse(this.chart.customStyle)
      tableChart.customAttr.color.tableHeaderBgColor = '#f8f8f9'
      tableChart.customAttr.color.tableItemBgColor = '#ffffff'
      tableChart.customAttr.color.tableFontColor = '#7c7e81'
      tableChart.customAttr.color.tableStripe = true
      tableChart.customStyle.text.show = false
      tableChart.customAttr = JSON.stringify(tableChart.customAttr)
      tableChart.customStyle = JSON.stringify(tableChart.customStyle)
      eventBus.$emit('openChartDetailsDialog', { chart: this.chart, tableChart: tableChart })
    },

    chartClick(param) {
      if (this.drillClickDimensionList.length < this.chart.drillFields.length - 1) {
        this.chart.type === 'map' && this.sendToChildren(param)
        this.drillClickDimensionList.push({ dimensionList: param.data.dimensionList })
        this.getData(this.element.propValue.viewId)
      } else if (this.chart.drillFields.length > 0) {
        this.$message({
          type: 'error',
          message: this.$t('chart.last_layer'),
          showClose: true
        })
      }
    },

    jumpClick(param) {
      let dimension, jumpInfo, sourceInfo
      // 倒序取最后一个能匹配的
      for (let i = param.dimensionList.length - 1; i >= 0; i--) {
        dimension = param.dimensionList[i]
        sourceInfo = param.viewId + '#' + dimension.id
        jumpInfo = this.nowPanelJumpInfo[sourceInfo]
        if (jumpInfo) {
          break
        }
      }
      if (jumpInfo) {
        param.sourcePanelId = this.panelInfo.id
        param.sourceViewId = param.viewId
        param.sourceFieldId = dimension.id
        // 内部仪表板跳转
        if (jumpInfo.linkType === 'inner') {
          if (jumpInfo.targetPanelId) {
            const url = '#/preview/' + jumpInfo.targetPanelId
            localStorage.setItem('jumpInfoParam', JSON.stringify(param))
            window.open(url, jumpInfo.jumpType)
          } else {
            this.$message({
              type: 'warn',
              message: '未指定跳转仪表板',
              showClose: true
            })
          }
        } else {
          const url = jumpInfo.content
          window.open(url, jumpInfo.jumpType)
        }
      } else {
        this.$message({
          type: 'warn',
          message: '未获取跳转信息',
          showClose: true
        })
      }
      // console.log('param=>' + JSON.stringify(param))
    },

    resetDrill() {
      const length = this.drillClickDimensionList.length
      this.drillClickDimensionList = []
      if (this.chart.type === 'map') {
        this.backToParent(0, length)
        const current = this.$refs[this.element.propValue.id]
        current && current.registerDynamicMap && current.registerDynamicMap(null)
      }
    },

    drillJump(index) {
      const length = this.drillClickDimensionList.length
      this.drillClickDimensionList = this.drillClickDimensionList.slice(0, index)
      if (this.chart.type === 'map') {
        this.backToParent(index, length)
      }
      this.getData(this.element.propValue.viewId)
    },
    // 回到父级地图
    backToParent(index, length) {
      if (length <= 0) return
      const times = length - 1 - index

      let temp = times
      let tempNode = this.currentAcreaNode
      while (temp >= 0) {
        tempNode = this.findEntityByCode(tempNode.pcode, this.places)
        temp--
      }

      this.currentAcreaNode = tempNode
      const current = this.$refs[this.element.propValue.id]
      current && current.registerDynamicMap && current.registerDynamicMap(this.currentAcreaNode.code)
      // this.$refs.dynamicChart && this.$refs.dynamicChart.registerDynamicMap && this.$refs.dynamicChart.registerDynamicMap(this.currentAcreaNode.code)
    },

    // 切换下一级地图
    sendToChildren(param) {
      const length = param.data.dimensionList.length
      const name = param.data.dimensionList[length - 1].value
      let aCode = null
      if (this.currentAcreaNode) {
        aCode = this.currentAcreaNode.code
      }
      //   const aCode = this.currentAcreaNode ? this.currentAcreaNode.code : null
      const customAttr = JSON.parse(this.chart.customAttr)
      const currentNode = this.findEntityByCode(aCode || customAttr.areaCode, this.places)
      if (currentNode && currentNode.children && currentNode.children.length > 0) {
        const nextNode = currentNode.children.find(item => item.name === name)
        // this.view.customAttr.areaCode = nextNode.code
        this.currentAcreaNode = nextNode
        const current = this.$refs[this.element.propValue.id]
        current && current.registerDynamicMap && current.registerDynamicMap(nextNode.code)
      }
    },

    findEntityByCode(code, array) {
      if (array === null || array.length === 0) array = this.places
      for (let index = 0; index < array.length; index++) {
        const node = array[index]
        if (node.code === code) return node
        if (node.children && node.children.length > 0) {
          const temp = this.findEntityByCode(code, node.children)
          if (temp) return temp
        }
      }
    },
    initAreas() {
    //   let mapping
    //   if ((mapping = localStorage.getItem('areaMapping')) !== null) {
    //     this.places = JSON.parse(mapping)
    //     return
    //   }
      Object.keys(this.places).length === 0 && areaMapping().then(res => {
        this.places = res.data
        // localStorage.setItem('areaMapping', JSON.stringify(res.data))
      })
    },
    doMapLink(linkFilters) {
      if (!linkFilters && linkFilters.length === 0) return
      const value = linkFilters[0].value
      if (!value && value.length === 0) return
      const name = value[0]
      if (!name) return
      const areaNode = this.findEntityByname(name, [])
      if (!areaNode) return
      const current = this.$refs[this.element.propValue.id]
      current && current.registerDynamicMap && current.registerDynamicMap(areaNode.code)
    },
    // 根据地名获取areaCode
    findEntityByname(name, array) {
      if (array === null || array.length === 0) array = this.places
      for (let index = 0; index < array.length; index++) {
        const node = array[index]
        if (node.name === name) return node
        if (node.children && node.children.length > 0) {
          const temp = this.findEntityByname(name, node.children)
          if (temp) return temp
        }
      }
    },
    destroyTimeMachine() {
      this.timeMachine && clearTimeout(this.timeMachine)
      this.timeMachine = null
    },

    // 边框变化
    chartResize(index) {
      if (this.$refs[this.element.propValue.id]) {
        this.timeMachine = setTimeout(() => {
          if (index === this.changeIndex) {
            this.$refs[this.element.propValue.id].chartResize()
          }
          this.destroyTimeMachine()
        }, 50)
      }
    },

    renderComponent() {
      return this.chart.render
    }
  }
}
</script>

<style lang="scss" scoped>
.rect-shape {
    width: 100%;
    height: 100%;
    overflow: hidden;
}
.chart-class{
  height: 100%;
}
.table-class{
  height: 100%;
}
.chart-error-class{
  text-align: center;
  height: calc(100% - 84px);
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #ece7e7;
}
.active {

}

.active >>> .icon-fangda{
  z-index: 2;
  display:block!important;
}

.rect-shape > i{
  right: 5px;
  color: gray;
  position: absolute;
}

.rect-shape >>> i:hover {
  color: red;
}

.rect-shape:hover >>> .icon-fangda {
  z-index: 2;
  display:block;
}

.rect-shape>>>.icon-fangda {
  display:none
}

.rect-shape:hover >>> .icon-shezhi {
  z-index: 2;
  display:block;
}

.rect-shape>>>.icon-shezhi {
  display:none
}
</style>
