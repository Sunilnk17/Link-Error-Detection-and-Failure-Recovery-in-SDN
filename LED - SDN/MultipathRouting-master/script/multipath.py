from mininet.topo import Topo
class MininetTopo(Topo):
  def __init__(self,**opts):
    Topo.__init__(self, **opts)
    host1 = self.addHost('h1')
    host2 = self.addHost('h2')
    host3 = self.addHost('h3')
    host4 = self.addHost('h4')

    self.switch = {}
    for s in range(1,23):
      self.switch[s-1] = self.addSwitch('s%s' %(s))
    self.addLink(self.switch[0], host1)
    self.addLink(self.switch[0], host2)

    self.addLink(self.switch[6], host3)
    self.addLink(self.switch[6], host4)

    self.addLink(self.switch[0], self.switch[1])
    self.addLink(self.switch[0], self.switch[2])
    self.addLink(self.switch[0], self.switch[3])
    self.addLink(self.switch[0], self.switch[4])

    self.addLink(self.switch[1], self.switch[5])
    self.addLink(self.switch[1], self.switch[7])
    self.addLink(self.switch[2], self.switch[5])
    self.addLink(self.switch[2], self.switch[7])

    self.addLink(self.switch[3], self.switch[8])
    self.addLink(self.switch[3], self.switch[9])
    self.addLink(self.switch[4], self.switch[8])
    self.addLink(self.switch[4], self.switch[9])

    self.addLink(self.switch[5], self.switch[10])
    self.addLink(self.switch[5], self.switch[11])
    self.addLink(self.switch[7], self.switch[12])
    self.addLink(self.switch[7], self.switch[13])
    self.addLink(self.switch[8], self.switch[14])
    self.addLink(self.switch[8], self.switch[15])
    self.addLink(self.switch[9], self.switch[16])
    self.addLink(self.switch[9], self.switch[17])

    self.addLink(self.switch[10], self.switch[18])
    self.addLink(self.switch[11], self.switch[18])
    self.addLink(self.switch[12], self.switch[19])
    self.addLink(self.switch[13], self.switch[19])
    self.addLink(self.switch[14], self.switch[20])
    self.addLink(self.switch[15], self.switch[20])
    self.addLink(self.switch[16], self.switch[21])
    self.addLink(self.switch[17], self.switch[21])

    self.addLink(self.switch[18], self.switch[6])
    self.addLink(self.switch[19], self.switch[6])
    self.addLink(self.switch[20], self.switch[6])
    self.addLink(self.switch[21], self.switch[6])

topos = {'multipath':(lambda:MininetTopo())}

